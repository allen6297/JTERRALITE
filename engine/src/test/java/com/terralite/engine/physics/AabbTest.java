package com.terralite.engine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AabbTest {
    @Test
    void aabbIntersectsOverlappingAndTouchingBounds() {
        Aabb bounds = new Aabb(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

        assertTrue(bounds.intersects(new Aabb(0.5, 0.5, 0.5, 2.0, 2.0, 2.0)));
        assertTrue(bounds.intersects(new Aabb(1.0, 1.0, 1.0, 2.0, 2.0, 2.0)));
    }

    @Test
    void aabbDoesNotIntersectSeparatedBounds() {
        Aabb bounds = new Aabb(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

        assertFalse(bounds.intersects(new Aabb(1.1, 0.0, 0.0, 2.0, 1.0, 1.0)));
    }

    @Test
    void aabbRejectsInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> new Aabb(1.0, 0.0, 0.0, 0.0, 1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Aabb(0.0, 1.0, 0.0, 1.0, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Aabb(0.0, 0.0, 1.0, 1.0, 1.0, 0.0));
    }
}
