package com.terralite.launcher.net;

import com.terralite.core.logging.Loggers;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.world.World;
import org.slf4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Connects to a {@link NetworkServer}, mirrors its world state into a local {@link World},
 * and forwards block interactions back to the server.
 *
 * <p>Call {@link #applyPending(World)} from the game thread each frame to apply queued
 * world updates without racing against the read thread.
 */
public final class NetworkClient {
    private static final Logger log = Loggers.get(NetworkClient.class);
    private static final int CHUNK_SIZE = com.terralite.runtime.world.RuntimeWorldFactory.CHUNK_SIZE;

    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter writer;
    private volatile boolean connected = false;
    private final ConcurrentLinkedQueue<NetMessage> pending = new ConcurrentLinkedQueue<>();
    private Consumer<ChunkPos> onChunkReady = pos -> {};
    private Consumer<ChunkPos> onBlockChange = pos -> {};
    private Consumer<PlayerJoinMessage> onPlayerJoin = msg -> {};
    private Consumer<PlayerLeaveMessage> onPlayerLeave = msg -> {};
    private Consumer<PlayerPositionMessage> onPlayerPosition = msg -> {};

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Callback invoked on the game thread (inside {@link #applyPending}) when a chunk arrives. */
    public NetworkClient onChunkReady(Consumer<ChunkPos> callback) {
        this.onChunkReady = callback;
        return this;
    }

    /** Callback invoked on the game thread when a server-pushed block change arrives. */
    public NetworkClient onBlockChange(Consumer<ChunkPos> callback) {
        this.onBlockChange = callback;
        return this;
    }

    public NetworkClient onPlayerJoin(Consumer<PlayerJoinMessage> callback) {
        this.onPlayerJoin = callback;
        return this;
    }

    public NetworkClient onPlayerLeave(Consumer<PlayerLeaveMessage> callback) {
        this.onPlayerLeave = callback;
        return this;
    }

    public NetworkClient onPlayerPosition(Consumer<PlayerPositionMessage> callback) {
        this.onPlayerPosition = callback;
        return this;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        writer = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())), false);
        connected = true;
        log.info("Connected to {}:{}", host, port);

        Thread reader = new Thread(this::readLoop, "net-read");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    pending.add(MessageCodec.decode(line));
                } catch (IOException e) {
                    log.warn("Malformed message: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            if (connected) log.info("Disconnected from server");
        } finally {
            connected = false;
        }
    }

    /**
     * Apply all queued server messages to {@code world}.
     * Must be called from the game thread.
     */
    public void applyPending(World world) {
        NetMessage msg;
        while ((msg = pending.poll()) != null) {
            switch (msg) {
                case ChunkDataMessage chunk -> {
                    ChunkPos pos = ChunkPos.of(chunk.cx(), chunk.cy(), chunk.cz());
                    if (!world.containsChunk(pos)) world.putChunk(new Chunk(pos));
                    for (var entry : chunk.blocks()) {
                        world.setBlock(
                            BlockPos.of(entry.x(), entry.y(), entry.z()),
                            new BlockState(ResourceId.id(entry.id()), entry.properties()));
                    }
                    onChunkReady.accept(pos);
                }
                case BlockChangeMessage change -> {
                    BlockPos bpos = BlockPos.of(change.x(), change.y(), change.z());
                    BlockState state = new BlockState(ResourceId.id(change.id()), change.properties());
                    if (state.isAir()) {
                        world.removeBlock(bpos);
                    } else {
                        world.setBlock(bpos, state);
                    }
                    ChunkPos cpos = ChunkPos.of(
                        Math.floorDiv(bpos.x(), CHUNK_SIZE),
                        Math.floorDiv(bpos.y(), CHUNK_SIZE),
                        Math.floorDiv(bpos.z(), CHUNK_SIZE));
                    onBlockChange.accept(cpos);
                }
                case PlayerJoinMessage join         -> onPlayerJoin.accept(join);
                case PlayerLeaveMessage leave       -> onPlayerLeave.accept(leave);
                case PlayerPositionMessage position -> onPlayerPosition.accept(position);
                default -> {}
            }
        }
    }

    /** Send local player position and look direction to the server. */
    public void sendPosition(double x, double y, double z, double yaw, double pitch) {
        send(new PlayerMoveMessage(x, y, z, yaw, pitch));
    }

    /** Send a place-block request to the server. */
    public void placeBlock(BlockPos pos, BlockState state) {
        send(new SetBlockMessage(pos.x(), pos.y(), pos.z(), state.id().toString()));
    }

    /** Send a break-block request to the server. */
    public void breakBlock(BlockPos pos) {
        send(new RemoveBlockMessage(pos.x(), pos.y(), pos.z()));
    }

    public boolean isConnected() { return connected; }

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private void send(NetMessage msg) {
        if (!connected) return;
        try {
            writer.println(MessageCodec.encode(msg));
            writer.flush();
        } catch (IOException e) {
            connected = false;
        }
    }
}
