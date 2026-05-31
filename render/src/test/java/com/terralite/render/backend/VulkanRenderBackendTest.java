package com.terralite.render.backend;

import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VulkanRenderBackendTest {
    @Test
    void meshSignatureChangesWhenChunkMeshVerticesChange() {
        RenderChunk chunk = new RenderChunk(0, 0, 0);
        VulkanRenderBackend.MeshSignature first = VulkanRenderBackend.MeshSignature.from(new RenderChunkMesh(
                chunk,
                new DebugMesh(List.of(
                        new DebugVertex(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f),
                        new DebugVertex(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f),
                        new DebugVertex(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f)
                ))
        ));
        VulkanRenderBackend.MeshSignature second = VulkanRenderBackend.MeshSignature.from(new RenderChunkMesh(
                chunk,
                new DebugMesh(List.of(
                        new DebugVertex(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f),
                        new DebugVertex(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f),
                        new DebugVertex(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
                ))
        ));

        assertEquals(first.vertexCount(), second.vertexCount());
        assertNotEquals(first, second);
    }
}
