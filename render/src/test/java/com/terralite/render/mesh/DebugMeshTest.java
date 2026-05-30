package com.terralite.render.mesh;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DebugMeshTest {
    @Test
    void triangleProvidesThreeVertices() {
        DebugMesh triangle = DebugMesh.triangle();

        assertEquals(3, triangle.vertices().size());
    }

    @Test
    void meshRejectsEmptyVertices() {
        assertThrows(IllegalArgumentException.class, () -> new DebugMesh(List.of()));
    }

    @Test
    void squareRejectsInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> DebugMesh.square(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f));
    }
}
