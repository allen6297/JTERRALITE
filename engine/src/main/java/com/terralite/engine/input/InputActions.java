package com.terralite.engine.input;

public final class InputActions {
    public static final InputAction MOVE_FORWARD = InputAction.of("terralite:move_forward");
    public static final InputAction MOVE_BACK = InputAction.of("terralite:move_back");
    public static final InputAction MOVE_LEFT = InputAction.of("terralite:move_left");
    public static final InputAction MOVE_RIGHT = InputAction.of("terralite:move_right");
    public static final InputAction JUMP = InputAction.of("terralite:jump");
    public static final InputAction SPRINT = InputAction.of("terralite:sprint");
    public static final InputAction BREAK_BLOCK = InputAction.of("terralite:break_block");
    public static final InputAction PLACE_BLOCK = InputAction.of("terralite:place_block");
    public static final InputAction TOGGLE_CAMERA = InputAction.of("terralite:toggle_camera");

    private InputActions() {
    }
}
