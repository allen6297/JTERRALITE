package com.terralite.engine.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityTest {
    @Test
    void scaleReturnsScaledVelocity() {
        Velocity velocity = new Velocity(2.0, -4.0, 0.5);

        assertEquals(new Velocity(1.0, -2.0, 0.25), velocity.scale(0.5));
    }
}
