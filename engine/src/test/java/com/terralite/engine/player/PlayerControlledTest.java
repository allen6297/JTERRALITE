package com.terralite.engine.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerControlledTest {
    @Test
    void playerControlledStoresMovementSpeed() {
        assertEquals(4.0, new PlayerControlled(4.0).movementSpeed());
    }

    @Test
    void playerControlledRejectsNegativeSpeed() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerControlled(-1.0));
    }
}
