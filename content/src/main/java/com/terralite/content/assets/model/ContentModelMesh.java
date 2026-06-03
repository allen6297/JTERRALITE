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

    public float minX() {
        return vertices.stream().map(ContentModelVertex::x).min(Float::compare).orElseThrow();
    }

    public float minY() {
        return vertices.stream().map(ContentModelVertex::y).min(Float::compare).orElseThrow();
    }

    public float minZ() {
        return vertices.stream().map(ContentModelVertex::z).min(Float::compare).orElseThrow();
    }

    public float maxX() {
        return vertices.stream().map(ContentModelVertex::x).max(Float::compare).orElseThrow();
    }

    public float maxY() {
        return vertices.stream().map(ContentModelVertex::y).max(Float::compare).orElseThrow();
    }

    public float maxZ() {
        return vertices.stream().map(ContentModelVertex::z).max(Float::compare).orElseThrow();
    }

    public float width() {
        return maxX() - minX();
    }

    public float height() {
        return maxY() - minY();
    }

    public float depth() {
        return maxZ() - minZ();
    }
}
