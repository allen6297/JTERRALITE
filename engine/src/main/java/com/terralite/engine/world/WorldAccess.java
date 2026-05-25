package com.terralite.engine.world;

import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;

import java.util.Collection;
import java.util.Optional;

public interface WorldAccess {
    Optional<Chunk> getChunk(ChunkPos pos);

    Chunk requireChunk(ChunkPos pos);

    boolean containsChunk(ChunkPos pos);

    Collection<ChunkPos> chunkPositions();
}
