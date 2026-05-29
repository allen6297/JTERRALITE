package com.terralite.render;

public record RenderStats(long frameIndex, Viewport viewport) {
    public RenderStats {
        if (frameIndex <= 0) {
            throw new IllegalArgumentException("Frame index must be positive");
        }
    }
}
