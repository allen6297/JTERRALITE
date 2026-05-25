package com.terralite.engine.chunk;

public record ChunkPos(int x, int y, int z) {
    public static ChunkPos of(int x, int y, int z) {
        return new ChunkPos(x, y, z);
    }
}
