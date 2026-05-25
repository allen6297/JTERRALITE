package com.terralite.engine.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputActionTest {
    @Test
    void actionStoresName() {
        assertEquals("terralite:jump", InputAction.of("terralite:jump").name());
    }

    @Test
    void actionRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> InputAction.of(" "));
    }
}
