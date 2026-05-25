package com.terralite.engine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformTest {
    @Test
    void translateReturnsMovedTransform() {
        Transform transform = new Transform(1.0, 2.0, 3.0);

        assertEquals(new Transform(5.0, 1.0, 5.5), transform.translate(4.0, -1.0, 2.5));
    }
}
