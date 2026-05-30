package com.terralite.render.mesh;

import java.util.List;
import java.util.Objects;

public record DebugMesh(List<DebugVertex> vertices) {
    public DebugMesh {
        vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("Debug mesh must contain at least one vertex");
        }
    }

    public static DebugMesh triangle() {
        return new DebugMesh(List.of(
                new DebugVertex( 0.0f,  1.0f, 0.0f, 1.0f, 0.2f, 0.2f),
                new DebugVertex(-1.0f, -1.0f, 0.0f, 0.2f, 1.0f, 0.4f),
                new DebugVertex( 1.0f, -1.0f, 0.0f, 0.2f, 0.5f, 1.0f)
        ));
    }

    /**
     * Axis-aligned square centered at (centerX, centerY, centerZ) with the given half-size,
     * lying in the XY plane (Z is constant). Two triangles, six vertices.
     */
    public static DebugMesh square(
            float centerX, float centerY, float centerZ,
            float halfSize,
            float red, float green, float blue
    ) {
        if (halfSize <= 0.0f) {
            throw new IllegalArgumentException("Square half size must be positive");
        }

        float left   = centerX - halfSize;
        float right  = centerX + halfSize;
        float bottom = centerY - halfSize;
        float top    = centerY + halfSize;
        float z      = centerZ;

        return new DebugMesh(List.of(
                new DebugVertex(left,  bottom, z, red, green, blue),
                new DebugVertex(right, bottom, z, red, green, blue),
                new DebugVertex(right, top,    z, red, green, blue),
                new DebugVertex(left,  bottom, z, red, green, blue),
                new DebugVertex(right, top,    z, red, green, blue),
                new DebugVertex(left,  top,    z, red, green, blue)
        ));
    }
}
