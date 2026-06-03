package com.terralite.launcher.input;

import com.terralite.engine.input.InputAction;
import com.terralite.engine.input.InputActions;
import com.terralite.engine.input.InputState;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

/**
 * Wires GLFW key-press/release events into the engine's {@link InputState}.
 */
public final class GlfwInputBridge {
    private static final Map<Integer, InputAction> KEY_BINDINGS = Map.ofEntries(
            Map.entry(GLFW.GLFW_KEY_W,             InputActions.MOVE_FORWARD),
            Map.entry(GLFW.GLFW_KEY_S,             InputActions.MOVE_BACK),
            Map.entry(GLFW.GLFW_KEY_A,             InputActions.MOVE_LEFT),
            Map.entry(GLFW.GLFW_KEY_D,             InputActions.MOVE_RIGHT),
            Map.entry(GLFW.GLFW_KEY_SPACE,         InputActions.JUMP),
            Map.entry(GLFW.GLFW_KEY_LEFT_SHIFT,    InputActions.SPRINT),
            Map.entry(GLFW.GLFW_KEY_F5,            InputActions.TOGGLE_CAMERA)
    );

    private final InputState inputState;
    private Runnable onEscape = () -> {};

    public GlfwInputBridge(InputState inputState) {
        this.inputState = inputState;
    }

    public void onEscape(Runnable handler) {
        this.onEscape = handler;
    }

    public void install(long windowHandle) {
        GLFW.glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                onEscape.run();
                return;
            }
            InputAction mapped = KEY_BINDINGS.get(key);
            if (mapped != null) {
                inputState.setPressed(mapped, action != GLFW.GLFW_RELEASE);
            }
        });

        GLFW.glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                inputState.setPressed(InputActions.BREAK_BLOCK, action != GLFW.GLFW_RELEASE);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                inputState.setPressed(InputActions.PLACE_BLOCK, action != GLFW.GLFW_RELEASE);
            }
        });
    }
}
