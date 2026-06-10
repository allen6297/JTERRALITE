package com.terralite.runtime.terrain;

import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;
import com.terralite.runtime.world.RuntimeWorldFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Generates terrain asynchronously.
 *
 * <p>Expensive noise math runs on a background thread pool. Completed chunk data
 * is queued and applied to the world on the simulation thread (safe, because
 * {@link World} block storage is not thread-safe).
 *
 * <p>Must run after {@link com.terralite.engine.terrain.ChunkLoaderSystem}.
 */
public final class TerrainGeneratorSystem implements WorldSimulationSystem {
    /** Max chunks applied to the world per simulation tick to keep the main thread responsive. */
    private static final int APPLY_PER_TICK = 1;

    private final TerrainGenerator generator;
    private final ExecutorService executor;
    private final Set<ChunkPos> submitted = new HashSet<>();
    private final ConcurrentLinkedQueue<GeneratedChunk> ready = new ConcurrentLinkedQueue<>();
    private Consumer<ChunkPos> onChunkReady = pos -> {};

    public TerrainGeneratorSystem(TerrainGenerator generator) {
        this(generator, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    public TerrainGeneratorSystem(TerrainGenerator generator, int threads) {
        this.generator = Objects.requireNonNull(generator, "generator");
        this.executor  = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "terrain-gen");
            t.setDaemon(true);
            return t;
        });
    }

    /** Called on the simulation thread after each chunk's blocks are applied. */
    public TerrainGeneratorSystem onChunkReady(Consumer<ChunkPos> callback) {
        this.onChunkReady = Objects.requireNonNull(callback, "callback");
        return this;
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        // 1. Apply completed chunks — capped per tick to avoid stalling the main thread
        int applied = 0;
        GeneratedChunk chunk;
        while (applied < APPLY_PER_TICK && (chunk = ready.poll()) != null) {
            for (var placement : chunk.blocks()) {
                world.setBlock(placement.pos(), placement.state());
            }
            onChunkReady.accept(chunk.pos());
            applied++;
        }

        // 2. Submit newly loaded chunks to the thread pool
        for (ChunkPos pos : world.chunkPositions()) {
            if (submitted.contains(pos)) continue;
            submitted.add(pos);
            if (canSkip(pos)) continue;
            executor.submit(() -> ready.add(generate(pos)));
        }
    }

    /** Call after a world load so newly visible chunks outside the save get regenerated. */
    public void reset() {
        submitted.clear();
    }

    /**
     * Synchronously generates terrain for {@code pos} on the calling thread.
     * Marks it so the async system won't re-generate it later.
     * The chunk must already exist in {@code world}.
     */
    public void generateNow(World world, ChunkPos pos) {
        if (submitted.contains(pos)) return;
        submitted.add(pos);
        if (!canSkip(pos)) {
            GeneratedChunk chunk = generate(pos);
            for (var placement : chunk.blocks()) {
                world.setBlock(placement.pos(), placement.state());
            }
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    // -------------------------------------------------------------------------

    private GeneratedChunk generate(ChunkPos pos) {
        int cs     = RuntimeWorldFactory.CHUNK_SIZE;
        int startX = pos.x() * cs;
        int startZ = pos.z() * cs;
        int minY   = pos.y() * cs;
        int maxY   = minY + cs - 1;

        List<BlockPlacement> blocks = new ArrayList<>();
        for (int x = startX; x < startX + cs; x++) {
            for (int z = startZ; z < startZ + cs; z++) {
                generator.collectColumn(x, z, minY, maxY, blocks);
            }
        }
        return new GeneratedChunk(pos, blocks);
    }

    private boolean canSkip(ChunkPos pos) {
        int cs    = RuntimeWorldFactory.CHUNK_SIZE;
        int minY  = pos.y() * cs;
        if (minY <= 0) return false;
        int sx = pos.x() * cs, sz = pos.z() * cs;
        return generator.surfaceHeight(sx,      sz)      < minY
            && generator.surfaceHeight(sx + cs, sz)      < minY
            && generator.surfaceHeight(sx,      sz + cs) < minY
            && generator.surfaceHeight(sx + cs, sz + cs) < minY;
    }

    private record GeneratedChunk(ChunkPos pos, List<BlockPlacement> blocks) {}
}
