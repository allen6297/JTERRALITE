package com.terralite.render.mesh;

import com.terralite.render.RenderChunk;
import org.junit.jupiter.api.Test;

import static com.terralite.render.mesh.ChunkDebugMeshFactory.CHUNK_HALF_SIZE;
import static com.terralite.render.mesh.ChunkDebugMeshFactory.CHUNK_UNIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChunkDebugMeshFactoryTest {
    @Test
    void createsWorldSpaceSquareFromChunkCoordinates() {
        DebugMesh marker = ChunkDebugMeshFactory.create(new RenderChunk(2, 0, -1));

        assertEquals(6, marker.vertices().size());
        // center = (2 * CHUNK_UNIT, 0 * CHUNK_UNIT) → bottom-left vertex = center - halfSize
        float expectedX = 2 * CHUNK_UNIT - CHUNK_HALF_SIZE;
        float expectedY = 0 * CHUNK_UNIT - CHUNK_HALF_SIZE;
        float expectedZ = -1 * CHUNK_UNIT;
        assertEquals(expectedX, marker.vertices().get(0).x(), 0.001f);
        assertEquals(expectedY, marker.vertices().get(0).y(), 0.001f);
        assertEquals(expectedZ, marker.vertices().get(0).z(), 0.001f);
    }

    @Test
    void variesColorByChunkCoordinate() {
        DebugMesh first  = ChunkDebugMeshFactory.create(new RenderChunk(0, 0, 0));
        DebugMesh second = ChunkDebugMeshFactory.create(new RenderChunk(1, 0, 0));

        assertNotEquals(first.vertices().get(0).red(), second.vertices().get(0).red());
        assertEquals(first.vertices().get(0).green(), second.vertices().get(0).green());
        assertEquals(first.vertices().get(0).blue(), second.vertices().get(0).blue());
    }
}
