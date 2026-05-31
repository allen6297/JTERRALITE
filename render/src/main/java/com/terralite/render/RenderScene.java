package com.terralite.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RenderScene(
        RenderCamera camera,
        List<RenderChunk> chunks,
        List<RenderChunkMesh> chunkMeshes,
        List<RenderObject> objects
) {
    public RenderScene {
        Objects.requireNonNull(camera, "camera");
        chunks = List.copyOf(Objects.requireNonNull(chunks, "chunks"));
        chunkMeshes = List.copyOf(Objects.requireNonNull(chunkMeshes, "chunkMeshes"));
        objects = List.copyOf(Objects.requireNonNull(objects, "objects"));
    }

    public static RenderScene empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private RenderCamera camera = RenderCamera.atOrigin();
        private final List<RenderChunk> chunks = new ArrayList<>();
        private final List<RenderChunkMesh> chunkMeshes = new ArrayList<>();
        private final List<RenderObject> objects = new ArrayList<>();

        private Builder() {
        }

        public Builder camera(RenderCamera camera) {
            this.camera = Objects.requireNonNull(camera, "camera");
            return this;
        }

        public Builder addChunk(RenderChunk chunk) {
            chunks.add(Objects.requireNonNull(chunk, "chunk"));
            return this;
        }

        public Builder addChunks(java.util.List<RenderChunk> chunks) {
            chunks.forEach(this::addChunk);
            return this;
        }

        public Builder addChunkMesh(RenderChunkMesh chunkMesh) {
            chunkMeshes.add(Objects.requireNonNull(chunkMesh, "chunkMesh"));
            return this;
        }

        public Builder addChunkMeshes(java.util.List<RenderChunkMesh> chunkMeshes) {
            chunkMeshes.forEach(this::addChunkMesh);
            return this;
        }

        public Builder addObject(RenderObject object) {
            objects.add(Objects.requireNonNull(object, "object"));
            return this;
        }

        public Builder addObjects(java.util.List<RenderObject> objects) {
            objects.forEach(this::addObject);
            return this;
        }

        public RenderScene build() {
            return new RenderScene(camera, chunks, chunkMeshes, objects);
        }
    }
}
