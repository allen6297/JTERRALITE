package com.terralite.engine.input

object InputActions {
    @JvmField val MOVE_FORWARD: InputAction = InputAction.of("terralite:move_forward")
    @JvmField val MOVE_BACK: InputAction = InputAction.of("terralite:move_back")
    @JvmField val MOVE_LEFT: InputAction = InputAction.of("terralite:move_left")
    @JvmField val MOVE_RIGHT: InputAction = InputAction.of("terralite:move_right")
    @JvmField val JUMP: InputAction = InputAction.of("terralite:jump")
    @JvmField val SPRINT: InputAction = InputAction.of("terralite:sprint")
    @JvmField val BREAK_BLOCK: InputAction = InputAction.of("terralite:break_block")
    @JvmField val PLACE_BLOCK: InputAction = InputAction.of("terralite:place_block")
    @JvmField val TOGGLE_CAMERA: InputAction = InputAction.of("terralite:toggle_camera")
}
