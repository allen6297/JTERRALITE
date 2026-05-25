package com.terralite.engine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundsTest {
    @Test
    void boundsClampTransformsToRange() {
        Bounds bounds = new Bounds(0.0, -1.0, 2.0, 10.0, 1.0, 5.0);

        assertEquals(new Transform(10.0, -1.0, 3.0), bounds.clamp(new Transform(12.0, -2.0, 3.0)));
    }

    @Test
    void boundsReportsContainment() {
        Bounds bounds = new Bounds(0.0, 0.0, 0.0, 10.0, 10.0, 10.0);

        assertTrue(bounds.contains(new Transform(5.0, 5.0, 5.0)));
        assertTrue(bounds.contains(new Transform(0.0, 0.0, 0.0)));
        assertTrue(bounds.contains(new Transform(10.0, 10.0, 10.0)));
        assertFalse(bounds.contains(new Transform(10.1, 5.0, 5.0)));
    }

    @Test
    void boundsRejectInvalidRanges() {
        assertThrows(IllegalArgumentException.class, () -> new Bounds(1.0, 0.0, 0.0, 0.0, 1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Bounds(0.0, 1.0, 0.0, 1.0, 0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new Bounds(0.0, 0.0, 1.0, 1.0, 1.0, 0.0));
    }
}
