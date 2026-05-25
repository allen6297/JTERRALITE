package com.terralite.engine.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputBindingsTest {
    @Test
    void bindingsMapControlsToActions() {
        InputBindings bindings = new InputBindings();

        bindings.bind("keyboard:w", InputActions.MOVE_FORWARD);

        assertTrue(bindings.contains("keyboard:w"));
        assertEquals(InputActions.MOVE_FORWARD, bindings.actionFor("keyboard:w").orElseThrow());
        assertEquals(1, bindings.size());
    }

    @Test
    void bindingsReplaceExistingControls() {
        InputBindings bindings = new InputBindings();

        bindings.bind("keyboard:w", InputActions.MOVE_FORWARD);
        bindings.bind("keyboard:w", InputActions.JUMP);

        assertEquals(InputActions.JUMP, bindings.actionFor("keyboard:w").orElseThrow());
        assertEquals(1, bindings.size());
    }

    @Test
    void bindingsRejectBlankControls() {
        InputBindings bindings = new InputBindings();

        assertThrows(IllegalArgumentException.class, () -> bindings.bind(" ", InputActions.JUMP));
        assertFalse(bindings.contains("keyboard:space"));
    }
}
