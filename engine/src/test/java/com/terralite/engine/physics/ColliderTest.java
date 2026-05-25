package com.terralite.engine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColliderTest {
    @Test
    void colliderComputesBoundsAtTransform() {
        Collider collider = new Collider(1.0, 2.0, 3.0);

        assertEquals(
            new Aabb(9.0, 18.0, 27.0, 11.0, 22.0, 33.0),
            collider.boundsAt(new Transform(10.0, 20.0, 30.0))
        );
    }

    @Test
    void colliderRejectsNegativeHalfExtents() {
        assertThrows(IllegalArgumentException.class, () -> new Collider(-1.0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Collider(0.0, -1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Collider(0.0, 0.0, -1.0));
    }
}
