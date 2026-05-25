package com.terralite.engine.input;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputStateTest {
    @Test
    void stateTracksPressedActions() {
        InputState state = new InputState();

        state.press(InputActions.JUMP);

        assertTrue(state.isPressed(InputActions.JUMP));
        assertEquals(Set.of(InputActions.JUMP), state.pressedActions());

        state.release(InputActions.JUMP);
        assertFalse(state.isPressed(InputActions.JUMP));
    }

    @Test
    void setPressedUpdatesActionState() {
        InputState state = new InputState();

        state.setPressed(InputActions.MOVE_FORWARD, true);
        assertTrue(state.isPressed(InputActions.MOVE_FORWARD));

        state.setPressed(InputActions.MOVE_FORWARD, false);
        assertFalse(state.isPressed(InputActions.MOVE_FORWARD));
    }

    @Test
    void clearReleasesAllActions() {
        InputState state = new InputState();

        state.press(InputActions.MOVE_FORWARD);
        state.press(InputActions.JUMP);
        state.clear();

        assertTrue(state.pressedActions().isEmpty());
    }
}
