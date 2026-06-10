package com.terralite.launcher;

import com.terralite.core.logging.Loggers;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.world.World;
import com.terralite.game.content.GameContentLoadReport;
import com.terralite.game.content.GameContentLoader;
import com.terralite.launcher.net.NetworkServer;
import com.terralite.runtime.terrain.TerrainGenerator;
import com.terralite.runtime.terrain.TerrainGeneratorSystem;
import com.terralite.runtime.world.RuntimeWorldFactory;
import com.terralite.server.TerraliteServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;

final class ServerApp {
    private static final Logger log = Loggers.get(ServerApp.class);
    private static final long TICK_SLEEP_MILLIS = 16L;

    private final Path packsRoot;
    private final int port;

    ServerApp(Path packsRoot) {
        this(packsRoot, NetworkServer.DEFAULT_PORT);
    }

    ServerApp(Path packsRoot, int port) {
        this.packsRoot = Objects.requireNonNull(packsRoot, "packsRoot");
        this.port = port;
    }

    void run() throws Exception {
        GameContentLoadReport content = new GameContentLoader().load(packsRoot);
        log.info("Loaded {} content pack(s)", content.packs().size());

        World world = new RuntimeWorldFactory().createEmpty(content.gameData());

        NetworkServer networkServer = new NetworkServer(port);

        // Terrain generates asynchronously; broadcast each chunk when ready
        var terrainGenerator = new TerrainGenerator(System.currentTimeMillis());
        var terrainSystem = new TerrainGeneratorSystem(terrainGenerator)
                .onChunkReady(pos -> {
                    networkServer.broadcastChunk(pos, world);
                    // Also send to any client that connects after this chunk was generated
                    // (handled in sendWorldSnapshot at connect time)
                });

        // When a client connects, send them the full current world
        // This runs on the accept thread; schedule it to the game thread via a flag
        // For simplicity we use a concurrent approach: sessions check for new connections
        // in tick() and we send the snapshot from the game thread on the next tick.
        // We keep a flag per session by tracking newly-added sessions.

        TerraliteServer server = TerraliteServer.builder()
                .world(world)
                .contentPacks(content.packs())
                .addWorldSimulationSystem(terrainSystem)
                .build();

        // Block requests from clients: apply to world and re-broadcast
        networkServer.onBlockRequest((pos, state) -> {
            if (state.isAir()) {
                world.removeBlock(pos);
            } else {
                world.setBlock(pos, state);
            }
            networkServer.broadcastBlockChange(pos, state.isAir()
                    ? new BlockState(com.terralite.core.registry.ResourceId.id("terralite:air"))
                    : state);
        });

        // Pre-generate spawn area so clients immediately receive terrain on connect
        log.info("Pre-generating spawn area...");
        int cs = RuntimeWorldFactory.CHUNK_SIZE;
        int spawnCX = Math.floorDiv(8, cs);
        int spawnCZ = Math.floorDiv(8, cs);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    ChunkPos pos = ChunkPos.of(spawnCX + dx, dy, spawnCZ + dz);
                    if (!world.containsChunk(pos)) world.putChunk(new Chunk(pos));
                    terrainSystem.generateNow(world, pos);
                }
            }
        }
        log.info("Spawn area ready ({} chunks)", world.chunkPositions().size());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            server.stop();
            networkServer.stop();
        }));

        server.start();
        networkServer.start();
        log.info("Server running on port {}. Press Ctrl+C to stop.", port);

        // Track sessions we've already sent the world snapshot to
        java.util.Set<com.terralite.launcher.net.ClientSession> snapshotSent =
                java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

        long last = System.nanoTime();
        while (true) {
            Thread.sleep(TICK_SLEEP_MILLIS);
            long now = System.nanoTime();
            server.advance(now - last);
            last = now;

            // Send world snapshot and existing player list to newly connected clients
            for (var session : networkServer.sessions()) {
                if (snapshotSent.add(session)) {
                    log.info("Sending world snapshot to player {}...", session.playerId);
                    networkServer.sendWorldSnapshot(session, world);
                    // Tell the new client about already-connected players
                    for (var other : networkServer.sessions()) {
                        if (other != session) {
                            session.send(new com.terralite.launcher.net.PlayerJoinMessage(
                                    other.playerId, other.px(), other.py(), other.pz()));
                        }
                    }
                    // Tell existing clients about the new player
                    for (var other : networkServer.sessions()) {
                        if (other != session) {
                            other.send(new com.terralite.launcher.net.PlayerJoinMessage(
                                    session.playerId, session.px(), session.py(), session.pz()));
                        }
                    }
                }
            }

            networkServer.tick(world);
            snapshotSent.removeIf(s -> !s.isAlive());
        }
    }
}
