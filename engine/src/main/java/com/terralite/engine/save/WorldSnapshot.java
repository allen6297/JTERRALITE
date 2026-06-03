package com.terralite.engine.save;

import com.terralite.engine.chunk.ChunkPos;

import java.util.List;
import java.util.Objects;

public record WorldSnapshot(List<ChunkPos> chunks, List<EntitySnapshot> entities, List<BlockSnapshot> blocks) {
    public WorldSnapshot(List<ChunkPos> chunks, List<EntitySnapshot> entities) {
        this(chunks, entities, List.of());
    }

    public WorldSnapshot {
        Objects.requireNonNull(chunks, "chunks");
        Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(blocks, "blocks");
        chunks = List.copyOf(chunks);
        entities = List.copyOf(entities);
        blocks = List.copyOf(blocks);
    }
}
