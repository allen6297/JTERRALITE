package com.terralite.render;

import com.terralite.render.mesh.DebugMesh;

import java.util.Objects;

public record RenderChunkMesh(RenderChunk chunk, DebugMesh mesh) {
    public RenderChunkMesh {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(mesh, "mesh");
    }
}
