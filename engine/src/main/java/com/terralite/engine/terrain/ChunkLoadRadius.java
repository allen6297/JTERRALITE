package com.terralite.engine.terrain;

public record ChunkLoadRadius(int horizontal, int vertical) {
    public ChunkLoadRadius {
        if (horizontal < 0) {
            throw new IllegalArgumentException("Horizontal chunk load radius cannot be negative");
        }
        if (vertical < 0) {
            throw new IllegalArgumentException("Vertical chunk load radius cannot be negative");
        }
    }

    public static ChunkLoadRadius horizontal(int radius) {
        return new ChunkLoadRadius(radius, 0);
    }

    public static ChunkLoadRadius cubic(int radius) {
        return new ChunkLoadRadius(radius, radius);
    }
}
