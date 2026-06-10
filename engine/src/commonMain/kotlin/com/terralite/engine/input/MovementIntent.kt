package com.terralite.engine.input

@JvmRecord
data class MovementIntent(val x: Double, val y: Double, val z: Double) {
    companion object {
        @JvmField val NONE = MovementIntent(0.0, 0.0, 0.0)

        @JvmStatic fun from(input: InputState): MovementIntent {
            var x = 0.0; var z = 0.0
            if (input.isPressed(InputActions.MOVE_LEFT))    x -= 1.0
            if (input.isPressed(InputActions.MOVE_RIGHT))   x += 1.0
            if (input.isPressed(InputActions.MOVE_FORWARD)) z += 1.0
            if (input.isPressed(InputActions.MOVE_BACK))    z -= 1.0
            return MovementIntent(x, if (input.isPressed(InputActions.JUMP)) 1.0 else 0.0, z)
        }
    }
}
