package com.terralite.engine.terrain;

public record BlockPos(int x, int y, int z) {
    public static BlockPos of(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }
}
