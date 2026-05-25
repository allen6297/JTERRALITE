package com.terralite.engine.chunk;

import java.util.Objects;

public record Chunk(ChunkPos pos) {
    public Chunk {
        Objects.requireNonNull(pos, "pos");
    }
}
