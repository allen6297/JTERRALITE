package com.terralite.render;

import com.terralite.render.mesh.DebugMesh;

import java.util.Objects;

/**
 * Describes a chunk mesh that needs to be uploaded to GPU memory this frame.
 */
public record PendingChunkUpload(
        RenderChunk chunk,
        MeshSignature signature,
        DebugMesh mesh
) {
    public PendingChunkUpload {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(signature, "signature");
        Objects.requireNonNull(mesh, "mesh");
    }
}