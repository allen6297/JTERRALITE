package com.terralite.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RenderScene(RenderCamera camera, List<RenderChunk> chunks, List<RenderObject> objects) {
    public RenderScene {
        Objects.requireNonNull(camera, "camera");
        chunks = List.copyOf(Objects.requireNonNull(chunks, "chunks"));
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

        public Builder addObject(RenderObject object) {
            objects.add(Objects.requireNonNull(object, "object"));
            return this;
        }

        public RenderScene build() {
            return new RenderScene(camera, chunks, objects);
        }
    }
}
