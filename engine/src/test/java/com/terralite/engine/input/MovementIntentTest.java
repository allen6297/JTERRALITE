package com.terralite.engine.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementIntentTest {
    @Test
    void movementIntentDefaultsToNone() {
        assertEquals(MovementIntent.NONE, MovementIntent.from(new InputState()));
    }

    @Test
    void movementIntentCombinesPressedActions() {
        InputState state = new InputState();

        state.press(InputActions.MOVE_FORWARD);
        state.press(InputActions.MOVE_LEFT);
        state.press(InputActions.JUMP);

        assertEquals(new MovementIntent(-1.0, 1.0, 1.0), MovementIntent.from(state));
    }

    @Test
    void opposingMovementActionsCancelOut() {
        InputState state = new InputState();

        state.press(InputActions.MOVE_FORWARD);
        state.press(InputActions.MOVE_BACK);
        state.press(InputActions.MOVE_LEFT);
        state.press(InputActions.MOVE_RIGHT);

        assertEquals(MovementIntent.NONE, MovementIntent.from(state));
    }
}
