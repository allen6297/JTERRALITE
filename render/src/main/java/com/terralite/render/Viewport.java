package com.terralite.render;

public record Viewport(int width, int height) {
    public Viewport {
        if (width <= 0) {
            throw new IllegalArgumentException("Viewport width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Viewport height must be positive");
        }
    }

    public double aspectRatio() {
        return (double) width / (double) height;
    }
}
