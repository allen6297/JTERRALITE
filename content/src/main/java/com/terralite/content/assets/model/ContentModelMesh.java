package com.terralite.content.assets.model;

import java.util.List;
import java.util.Objects;

public record ContentModelMesh(List<ContentModelVertex> vertices) {
    public ContentModelMesh {
        vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("Content model mesh must contain at least one vertex");
        }
        if (vertices.size() % 3 != 0) {
            throw new IllegalArgumentException("Content model mesh vertices must form triangles");
        }
    }

    public int triangleCount() {
        return vertices.size() / 3;
    }
}
