package com.terralite.engine.terrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkLoadRadiusTest {
    @Test
    void horizontalRadiusKeepsVerticalRadiusAtZero() {
        assertEquals(new ChunkLoadRadius(2, 0), ChunkLoadRadius.horizontal(2));
    }

    @Test
    void cubicRadiusUsesSameHorizontalAndVerticalRadius() {
        assertEquals(new ChunkLoadRadius(2, 2), ChunkLoadRadius.cubic(2));
    }

    @Test
    void radiusRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkLoadRadius(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkLoadRadius(0, -1));
    }
}
