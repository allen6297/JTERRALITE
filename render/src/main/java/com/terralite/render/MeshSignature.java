package com.terralite.render;

import com.terralite.render.mesh.DebugMesh;

import java.util.Objects;

/**
 * Lightweight identity for a chunk mesh. Uses object identity rather than content hash
 * so that reference equality reliably detects changes when the pipeline reuses the same
 * {@link RenderChunkMesh} instance across frames.
 */
public record MeshSignature(int vertexCount, int objectId) {

    public static MeshSignature untracked() {
        return new MeshSignature(-1, 0);
    }

    /**
     * Creates a signature from a {@link RenderChunkMesh} using its reference identity.
     */
    public static MeshSignature from(RenderChunkMesh chunkMesh) {
        Objects.requireNonNull(chunkMesh, "chunkMesh");
        return new MeshSignature(
                chunkMesh.mesh().vertices().size(),
                System.identityHashCode(chunkMesh)
        );
    }

    /**
     * Creates a signature from a {@link DebugMesh} directly.
     */
    public static MeshSignature from(DebugMesh mesh) {
        Objects.requireNonNull(mesh, "mesh");
        return new MeshSignature(mesh.vertices().size(), System.identityHashCode(mesh));
    }
}