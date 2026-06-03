package com.terralite.engine.world;

import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.chunk.ChunkStorage;
import com.terralite.engine.chunk.InMemoryChunkStorage;
import com.terralite.engine.entity.EntityManager;
import com.terralite.engine.entity.EntitySpawner;
import com.terralite.engine.entity.InMemoryEntityManager;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.terrain.BlockStorage;
import com.terralite.engine.terrain.SparseBlockStorage;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class World implements WorldAccess {
    private final ChunkStorage chunks;
    private final EntityManager entities;
    private final BlockStorage blocks;

    public World() {
        this(new InMemoryChunkStorage(), new InMemoryEntityManager(), new SparseBlockStorage());
    }

    public World(BlockStorage blocks) {
        this(new InMemoryChunkStorage(), new InMemoryEntityManager(), blocks);
    }

    public World(ChunkStorage chunks) {
        this(chunks, new InMemoryEntityManager(), new SparseBlockStorage());
    }

    public World(ChunkStorage chunks, EntityManager entities) {
        this(chunks, entities, new SparseBlockStorage());
    }

    public World(ChunkStorage chunks, EntityManager entities, BlockStorage blocks) {
        this.chunks = Objects.requireNonNull(chunks, "chunks");
        this.entities = Objects.requireNonNull(entities, "entities");
        this.blocks = Objects.requireNonNull(blocks, "blocks");
    }

    public ChunkStorage chunks() {
        return chunks;
    }

    public EntityManager entities() {
        return entities;
    }

    public BlockStorage blocks() {
        return blocks;
    }

    public EntitySpawner spawner() {
        return new EntitySpawner(this);
    }

    public BlockState getBlock(BlockPos pos) {
        return blocks.get(pos);
    }

    public void setBlock(BlockPos pos, BlockState state) {
        blocks.set(pos, state);
    }

    public Chunk putChunk(Chunk chunk) {
        return chunks.put(chunk);
    }

    public Chunk removeChunk(ChunkPos pos) {
        return chunks.remove(pos);
    }

    @Override
    public Optional<Chunk> getChunk(ChunkPos pos) {
        return chunks.get(pos);
    }

    @Override
    public Chunk requireChunk(ChunkPos pos) {
        return chunks.require(pos);
    }

    @Override
    public boolean containsChunk(ChunkPos pos) {
        return chunks.contains(pos);
    }

    @Override
    public Collection<ChunkPos> chunkPositions() {
        return chunks.positions();
    }
}
