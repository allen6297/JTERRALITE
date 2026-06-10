package com.terralite.render;

import com.terralite.render.math.FrustumCuller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of everything to render in a single frame.
 *
 * <p>Defensive copies are skipped in the compact constructor because this record
 * is typically constructed once per frame from a builder; the caller must not
 * mutate the lists after passing them in.
 */
public record RenderScene(
        RenderCamera camera,
        List<RenderChunk> chunks,
        List<RenderChunkMesh> chunkMeshes,
        List<RenderObject> objects,
        List<RenderChunkMesh> dynamicMeshes
) {
    public RenderScene {
        Objects.requireNonNull(camera, "camera");
        Objects.requireNonNull(chunks, "chunks");
        Objects.requireNonNull(chunkMeshes, "chunkMeshes");
        Objects.requireNonNull(objects, "objects");
        Objects.requireNonNull(dynamicMeshes, "dynamicMeshes");
    }

    /** Returns all meshes — chunk meshes followed by dynamic meshes — for backends that don't distinguish. */
    public List<RenderChunkMesh> allMeshes() {
        if (dynamicMeshes.isEmpty()) return chunkMeshes;
        if (chunkMeshes.isEmpty()) return dynamicMeshes;
        var all = new ArrayList<RenderChunkMesh>(chunkMeshes.size() + dynamicMeshes.size());
        all.addAll(chunkMeshes);
        all.addAll(dynamicMeshes);
        return all;
    }

    public static RenderScene empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns chunk meshes whose AABB is visible in the given frustum.
     * This is a convenience helper for callers that want to pre-filter
     * before submitting to the backend.
     */
    public List<RenderChunkMesh> visibleChunkMeshes(FrustumCuller culler, int chunkSize) {
        Objects.requireNonNull(culler, "culler");
        return chunkMeshes.stream()
                .filter(chunkMesh -> {
                    RenderChunk chunk = chunkMesh.chunk();
                    float minX = chunk.x() * chunkSize;
                    float minY = chunk.y() * chunkSize;
                    float minZ = chunk.z() * chunkSize;
                    float maxX = minX + chunkSize;
                    float maxY = minY + chunkSize;
                    float maxZ = minZ + chunkSize;
                    return culler.isVisible(minX, minY, minZ, maxX, maxY, maxZ);
                })
                .toList();
    }

    public static final class Builder {
        private RenderCamera camera = RenderCamera.atOrigin();
        private final List<RenderChunk> chunks = new ArrayList<>();
        private final List<RenderChunkMesh> chunkMeshes = new ArrayList<>();
        private final List<RenderObject> objects = new ArrayList<>();
        private final List<RenderChunkMesh> dynamicMeshes = new ArrayList<>();

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

        public Builder addChunks(List<RenderChunk> chunks) {
            chunks.forEach(this::addChunk);
            return this;
        }

        public Builder addChunkMesh(RenderChunkMesh chunkMesh) {
            chunkMeshes.add(Objects.requireNonNull(chunkMesh, "chunkMesh"));
            return this;
        }

        public Builder addChunkMeshes(List<RenderChunkMesh> chunkMeshes) {
            chunkMeshes.forEach(this::addChunkMesh);
            return this;
        }

        /**
         * Adds a dynamic mesh (player model, overlay, etc.) that must be re-uploaded every
         * frame and is not subject to the per-frame terrain upload throttle.
         */
        public Builder addDynamicMesh(RenderChunkMesh mesh) {
            dynamicMeshes.add(Objects.requireNonNull(mesh, "mesh"));
            return this;
        }

        public Builder addDynamicMeshes(List<RenderChunkMesh> meshes) {
            meshes.forEach(this::addDynamicMesh);
            return this;
        }

        public Builder addObject(RenderObject object) {
            objects.add(Objects.requireNonNull(object, "object"));
            return this;
        }

        public Builder addObjects(List<RenderObject> objects) {
            objects.forEach(this::addObject);
            return this;
        }

        public RenderScene build() {
            return new RenderScene(camera, chunks, chunkMeshes, objects, dynamicMeshes);
        }
    }
}