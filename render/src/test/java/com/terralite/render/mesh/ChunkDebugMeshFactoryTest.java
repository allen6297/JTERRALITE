package com.terralite.render.mesh;

import com.terralite.render.RenderChunk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChunkDebugMeshFactoryTest {
    @Test
    void createsChunkSpaceSquareFromChunkCoordinates() {
        DebugMesh marker = ChunkDebugMeshFactory.create(new RenderChunk(2, 0, -1));

        assertEquals(6, marker.vertices().size());
        assertEquals(0.16f, marker.vertices().get(0).x());
        assertEquals(-0.14f, marker.vertices().get(0).y());
    }

    @Test
    void variesColorByChunkCoordinate() {
        DebugMesh first = ChunkDebugMeshFactory.create(new RenderChunk(0, 0, 0));
        DebugMesh second = ChunkDebugMeshFactory.create(new RenderChunk(1, 0, 0));

        assertNotEquals(first.vertices().get(0).red(), second.vertices().get(0).red());
        assertEquals(first.vertices().get(0).green(), second.vertices().get(0).green());
        assertEquals(first.vertices().get(0).blue(), second.vertices().get(0).blue());
    }
}
