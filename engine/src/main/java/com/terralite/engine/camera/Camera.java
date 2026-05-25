package com.terralite.engine.camera;

import com.terralite.engine.physics.Transform;

import java.util.Objects;

public final class Camera {
    private Transform transform;
    private double fovDegrees;
    private double nearPlane;
    private double farPlane;

    public Camera() {
        this(Transform.ORIGIN, 70.0, 0.01, 1_000.0);
    }

    public Camera(Transform transform, double fovDegrees, double nearPlane, double farPlane) {
        this.transform = Objects.requireNonNull(transform, "transform");
        setProjection(fovDegrees, nearPlane, farPlane);
    }

    public Transform transform() {
        return transform;
    }

    public void setTransform(Transform transform) {
        this.transform = Objects.requireNonNull(transform, "transform");
    }

    public double fovDegrees() {
        return fovDegrees;
    }

    public double nearPlane() {
        return nearPlane;
    }

    public double farPlane() {
        return farPlane;
    }

    public void setProjection(double fovDegrees, double nearPlane, double farPlane) {
        if (fovDegrees <= 0.0 || fovDegrees >= 180.0) {
            throw new IllegalArgumentException("Camera fov must be between 0 and 180 degrees");
        }
        if (nearPlane <= 0.0) {
            throw new IllegalArgumentException("Camera near plane must be positive");
        }
        if (farPlane <= nearPlane) {
            throw new IllegalArgumentException("Camera far plane must be greater than near plane");
        }

        this.fovDegrees = fovDegrees;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
    }
}
