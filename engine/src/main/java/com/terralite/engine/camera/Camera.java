package com.terralite.engine.camera;

import com.terralite.engine.physics.Transform;

import java.util.Objects;

public final class Camera {
    private Transform transform;
    private double fovDegrees;
    private double nearPlane;
    private double farPlane;
    private double yaw;
    private double pitch;

    public Camera() {
        this(Transform.ORIGIN, 70.0, 0.01, 1_000.0, 0.0, 0.0);
    }

    public Camera(Transform transform, double fovDegrees, double nearPlane, double farPlane) {
        this(transform, fovDegrees, nearPlane, farPlane, 0.0, 0.0);
    }

    public Camera(Transform transform, double fovDegrees, double nearPlane, double farPlane,
                  double yaw, double pitch) {
        this.transform = Objects.requireNonNull(transform, "transform");
        setProjection(fovDegrees, nearPlane, farPlane);
        setOrientation(yaw, pitch);
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

    public double yaw() {
        return yaw;
    }

    public double pitch() {
        return pitch;
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

    public void setOrientation(double yaw, double pitch) {
        if (pitch < -89.0 || pitch > 89.0) {
            throw new IllegalArgumentException("Camera pitch must be between -89 and 89 degrees");
        }
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public void setPitch(double pitch) {
        if (pitch < -89.0 || pitch > 89.0) {
            throw new IllegalArgumentException("Camera pitch must be between -89 and 89 degrees");
        }
        this.pitch = pitch;
    }
}
