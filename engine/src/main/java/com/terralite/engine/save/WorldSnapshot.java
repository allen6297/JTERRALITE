package com.terralite.engine.save;

import com.terralite.engine.chunk.ChunkPos;

import java.util.List;
import java.util.Objects;

public record WorldSnapshot(List<ChunkPos> chunks, List<EntitySnapshot> entities) {
    public WorldSnapshot {
        Objects.requireNonNull(chunks, "chunks");
        Objects.requireNonNull(entities, "entities");
        chunks = List.copyOf(chunks);
        entities = List.copyOf(entities);
    }
}
