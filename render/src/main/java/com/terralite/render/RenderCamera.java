package com.terralite.render;

import java.util.Objects;

public record RenderCamera(
        double x,
        double y,
        double z,
        double fovDegrees,
        double nearPlane,
        double farPlane
) {
    public RenderCamera {
        if (fovDegrees <= 0.0 || fovDegrees >= 180.0) {
            throw new IllegalArgumentException("Camera fov must be between 0 and 180 degrees");
        }
        if (nearPlane <= 0.0) {
            throw new IllegalArgumentException("Camera near plane must be positive");
        }
        if (farPlane <= nearPlane) {
            throw new IllegalArgumentException("Camera far plane must be greater than near plane");
        }
    }

    public static RenderCamera atOrigin() {
        return new RenderCamera(0.0, 0.0, 0.0, 70.0, 0.01, 1_000.0);
    }
}
