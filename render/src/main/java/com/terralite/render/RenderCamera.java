package com.terralite.render;

/**
 * Camera parameters for a render frame.
 *
 * <p>{@code yaw} rotates horizontally around the Y axis (degrees, 0 = looking along -Z).
 * {@code pitch} tilts vertically (degrees, positive = looking up, clamped to ±89°).
 */
public record RenderCamera(
        float x,
        float y,
        float z,
        float fovDegrees,
        float nearPlane,
        float farPlane,
        float yaw,
        float pitch
) {
    public RenderCamera {
        if (fovDegrees <= 0.0f || fovDegrees >= 180.0f) {
            throw new IllegalArgumentException("Camera fov must be between 0 and 180 degrees");
        }
        if (nearPlane <= 0.0f) {
            throw new IllegalArgumentException("Camera near plane must be positive");
        }
        if (farPlane <= nearPlane) {
            throw new IllegalArgumentException("Camera far plane must be greater than near plane");
        }
        if (pitch < -89.0f || pitch > 89.0f) {
            throw new IllegalArgumentException("Camera pitch must be between -89 and 89 degrees");
        }
    }

    /** Convenience constructor — yaw=0 and pitch=0 (looking along -Z). */
    public RenderCamera(
            float x,
            float y,
            float z,
            float fovDegrees,
            float nearPlane,
            float farPlane
    ) {
        this(x, y, z, fovDegrees, nearPlane, farPlane, 0.0f, 0.0f);
    }

    public static RenderCamera atOrigin() {
        return new RenderCamera(0.0f, 0.0f, 0.0f, 70.0f, 0.01f, 1_000.0f);
    }
}