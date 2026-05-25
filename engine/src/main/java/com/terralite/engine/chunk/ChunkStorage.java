package com.terralite.engine.chunk;

import java.util.Collection;
import java.util.Optional;

public interface ChunkStorage {
    Optional<Chunk> get(ChunkPos pos);

    boolean contains(ChunkPos pos);

    Chunk put(Chunk chunk);

    Chunk remove(ChunkPos pos);

    Chunk require(ChunkPos pos);

    Collection<ChunkPos> positions();

    Collection<Chunk> chunks();

    int size();
}
