package com.terralite.launcher.input;

import com.terralite.engine.camera.Camera;
import org.lwjgl.glfw.GLFW;

/**
 * Captures the cursor and translates mouse deltas into camera yaw/pitch.
 */
public final class MouseLookController {
    private static final double SENSITIVITY = 0.12;
    private static final double MIN_PITCH = -89.0;
    private static final double MAX_PITCH = 89.0;

    private final Camera camera;
    private double lastX = Double.NaN;
    private double lastY = Double.NaN;
    private boolean captured = false;

    public MouseLookController(Camera camera) {
        this.camera = camera;
    }

    public void install(long windowHandle) {
        GLFW.glfwSetCursorPosCallback(windowHandle, (window, x, y) -> {
            if (!captured) return;
            if (Double.isNaN(lastX)) {
                lastX = x;
                lastY = y;
                return;
            }
            double dx = x - lastX;
            double dy = y - lastY;
            lastX = x;
            lastY = y;

            double newYaw = camera.yaw() - dx * SENSITIVITY;
            double newPitch = Math.max(MIN_PITCH, Math.min(MAX_PITCH,
                    camera.pitch() - dy * SENSITIVITY));
            camera.setYaw(newYaw);
            camera.setPitch(newPitch);
        });
    }

    public void capture(long windowHandle) {
        if (!captured) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            lastX = Double.NaN;
            lastY = Double.NaN;
            captured = true;
        }
    }

    public void release(long windowHandle) {
        if (captured) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            captured = false;
        }
    }

    public boolean isCaptured() {
        return captured;
    }
}
