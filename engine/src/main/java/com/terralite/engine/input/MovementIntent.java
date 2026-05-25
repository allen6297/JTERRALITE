package com.terralite.engine.input;

public record MovementIntent(double x, double y, double z) {
    public static final MovementIntent NONE = new MovementIntent(0.0, 0.0, 0.0);

    public static MovementIntent from(InputState input) {
        double x = 0.0;
        double z = 0.0;

        if (input.isPressed(InputActions.MOVE_LEFT)) {
            x -= 1.0;
        }
        if (input.isPressed(InputActions.MOVE_RIGHT)) {
            x += 1.0;
        }
        if (input.isPressed(InputActions.MOVE_FORWARD)) {
            z += 1.0;
        }
        if (input.isPressed(InputActions.MOVE_BACK)) {
            z -= 1.0;
        }

        return new MovementIntent(x, input.isPressed(InputActions.JUMP) ? 1.0 : 0.0, z);
    }
}
