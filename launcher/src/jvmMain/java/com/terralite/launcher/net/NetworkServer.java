package com.terralite.launcher.net;

import com.terralite.core.logging.Loggers;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.world.World;
import com.terralite.runtime.world.RuntimeWorldFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Listens for TCP connections on a port and synchronises world state with connected clients.
 *
 * <p>Call {@link #tick(World)} from the game loop each frame to process inbound block
 * requests and (optionally) forward them to the authoritative world.
 */
public final class NetworkServer {
    public static final int DEFAULT_PORT = 25565;
    private static final Logger log = Loggers.get(NetworkServer.class);

    private final int port;
    private final List<ClientSession> sessions = new CopyOnWriteArrayList<>();
    private ServerSocket serverSocket;

    /**
     * Called on the game thread when a client places or removes a block so the host can
     * apply and re-broadcast the change. Signature: (world, blockPos, newState).
     * newState is {@code BlockState.AIR} for removal.
     */
    private BiConsumer<BlockPos, BlockState> onBlockRequest = (pos, state) -> {};

    public NetworkServer(int port) {
        this.port = port;
    }

    public NetworkServer onBlockRequest(BiConsumer<BlockPos, BlockState> handler) {
        this.onBlockRequest = handler;
        return this;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        Thread accept = new Thread(this::acceptLoop, "net-accept");
        accept.setDaemon(true);
        accept.start();
        log.info("Listening on port {}", port);
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                String playerId = UUID.randomUUID().toString().substring(0, 8);
                log.info("Client connected: {} (id={})", socket.getRemoteSocketAddress(), playerId);
                ClientSession session = new ClientSession(playerId, socket);
                sessions.add(session);
                session.startReading();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) log.error("Accept error", e);
            }
        }
    }

    /**
     * Must be called from the game thread each tick.
     * Processes inbound messages, broadcasts positions, and cleans up dead sessions.
     */
    public void tick(World world) {
        List<ClientSession> dead = new ArrayList<>();
        for (ClientSession session : sessions) {
            if (!session.isAlive()) { dead.add(session); continue; }
            NetMessage msg;
            while ((msg = session.inbound.poll()) != null) {
                switch (msg) {
                    case SetBlockMessage set ->
                        onBlockRequest.accept(
                            BlockPos.of(set.x(), set.y(), set.z()),
                            new BlockState(ResourceId.id(set.id())));
                    case RemoveBlockMessage rem ->
                        onBlockRequest.accept(
                            BlockPos.of(rem.x(), rem.y(), rem.z()),
                            BlockState.AIR);
                    case PlayerMoveMessage move -> {
                        session.px = move.x();
                        session.py = move.y();
                        session.pz = move.z();
                        session.pyaw = move.yaw();
                        session.ppitch = move.pitch();
                    }
                    default -> {}
                }
            }
        }

        // Broadcast all player positions to all live clients
        for (ClientSession receiver : sessions) {
            if (!receiver.isAlive()) continue;
            for (ClientSession other : sessions) {
                if (other == receiver || !other.isAlive()) continue;
                receiver.send(new PlayerPositionMessage(
                        other.playerId, other.px, other.py, other.pz, other.pyaw, other.ppitch));
            }
        }

        // Remove dead sessions and notify remaining clients
        if (!dead.isEmpty()) {
            sessions.removeAll(dead);
            dead.forEach(s -> {
                s.close();
                log.info("Player {} disconnected", s.playerId);
                broadcast(new PlayerLeaveMessage(s.playerId));
            });
        }
    }

    /** Send the full contents of one chunk to all connected clients. */
    public void broadcastChunk(ChunkPos pos, World world) {
        if (sessions.isEmpty()) return;
        List<ChunkDataMessage.BlockEntry> blocks = collectChunkBlocks(pos, world);
        broadcast(new ChunkDataMessage(pos.x(), pos.y(), pos.z(), blocks));
    }

    /** Notify all clients that a single block changed. */
    public void broadcastBlockChange(BlockPos pos, BlockState state) {
        broadcast(new BlockChangeMessage(pos.x(), pos.y(), pos.z(), state.id().toString(), state.properties()));
    }

    /** Send a new client the complete current world state. */
    public void sendWorldSnapshot(ClientSession session, World world) {
        for (ChunkPos pos : world.chunkPositions()) {
            List<ChunkDataMessage.BlockEntry> blocks = collectChunkBlocks(pos, world);
            session.send(new ChunkDataMessage(pos.x(), pos.y(), pos.z(), blocks));
        }
    }

    /** Returns the most recently connected session, or {@code null} if nobody is connected. */
    public ClientSession latestSession() {
        List<ClientSession> live = sessions.stream().filter(ClientSession::isAlive).toList();
        return live.isEmpty() ? null : live.get(live.size() - 1);
    }

    public List<ClientSession> sessions() {
        return sessions.stream().filter(ClientSession::isAlive).toList();
    }

    public void stop() {
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        sessions.forEach(ClientSession::close);
        sessions.clear();
    }

    // -------------------------------------------------------------------------

    private static List<ChunkDataMessage.BlockEntry> collectChunkBlocks(ChunkPos pos, World world) {
        int cs = RuntimeWorldFactory.CHUNK_SIZE;
        int sx = pos.x() * cs, sy = pos.y() * cs, sz = pos.z() * cs;
        List<ChunkDataMessage.BlockEntry> blocks = new ArrayList<>();
        for (int x = sx; x < sx + cs; x++) {
            for (int y = sy; y < sy + cs; y++) {
                for (int z = sz; z < sz + cs; z++) {
                    BlockState state = world.getBlock(BlockPos.of(x, y, z));
                    if (!state.isAir()) {
                        blocks.add(new ChunkDataMessage.BlockEntry(x, y, z, state.id().toString(), state.properties()));
                    }
                }
            }
        }
        return blocks;
    }

    private void broadcast(NetMessage msg) {
        for (ClientSession s : sessions) s.send(msg);
    }
}
