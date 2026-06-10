package com.terralite.content.assets.model;

import java.util.List;
import java.util.Objects;

public record ContentModelMesh(List<ContentModelVertex> vertices, List<ContentModelBone> bones) {
    public ContentModelMesh {
        vertices = List.copyOf(Objects.requireNonNull(vertices, "vertices"));
        bones    = List.copyOf(Objects.requireNonNull(bones,    "bones"));
        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("Content model mesh must contain at least one vertex");
        }
        if (vertices.size() % 3 != 0) {
            throw new IllegalArgumentException("Content model mesh vertices must form triangles");
        }
    }

    /** Backward-compatible constructor for models without bone/animation data (OBJ, Terralite JSON). */
    public ContentModelMesh(List<ContentModelVertex> vertices) {
        this(vertices, List.of());
    }

    /**
     * Creates a mesh from a set of named bones.
     * The flat {@link #vertices()} list is derived by concatenating all bone vertex lists,
     * so existing block-rendering code continues to work without modification.
     */
    public static ContentModelMesh ofBones(List<ContentModelBone> bones) {
        Objects.requireNonNull(bones, "bones");
        List<ContentModelVertex> all = bones.stream()
                .flatMap(b -> b.vertices().stream())
                .toList();
        return new ContentModelMesh(all, bones);
    }

    /** {@code true} when this mesh carries per-bone data suitable for animation. */
    public boolean hasBonesData() {
        return !bones.isEmpty();
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
