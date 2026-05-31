package com.terralite.render.mesh;

import com.terralite.core.registry.ResourceId;

public record DebugVertex(
        float x,
        float y,
        float z,
        float red,
        float green,
        float blue,
        float u,
        float v,
        ResourceId texture
) {
    public DebugVertex(float x, float y, float z, float red, float green, float blue) {
        this(x, y, z, red, green, blue, 0.0f, 0.0f, null);
    }
}
