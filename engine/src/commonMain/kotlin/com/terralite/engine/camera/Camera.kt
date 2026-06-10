package com.terralite.engine.camera

import com.terralite.engine.physics.Transform

class Camera @JvmOverloads constructor(
    private var transform: Transform = Transform.ORIGIN,
    private var fovDegrees: Double = 70.0,
    private var nearPlane: Double = 0.01,
    private var farPlane: Double = 1_000.0,
    private var yaw: Double = 0.0,
    private var pitch: Double = 0.0
) {
    init {
        validateProjection(fovDegrees, nearPlane, farPlane)
        validatePitch(pitch)
    }

    fun transform(): Transform = transform
    fun fovDegrees(): Double = fovDegrees
    fun nearPlane(): Double = nearPlane
    fun farPlane(): Double = farPlane
    fun yaw(): Double = yaw
    fun pitch(): Double = pitch

    fun setTransform(transform: Transform) { this.transform = transform }

    fun setProjection(fovDegrees: Double, nearPlane: Double, farPlane: Double) {
        validateProjection(fovDegrees, nearPlane, farPlane)
        this.fovDegrees = fovDegrees
        this.nearPlane = nearPlane
        this.farPlane = farPlane
    }

    fun setOrientation(yaw: Double, pitch: Double) {
        validatePitch(pitch)
        this.yaw = yaw
        this.pitch = pitch
    }

    fun setYaw(yaw: Double) { this.yaw = yaw }

    fun setPitch(pitch: Double) {
        validatePitch(pitch)
        this.pitch = pitch
    }

    private companion object {
        fun validateProjection(fovDegrees: Double, nearPlane: Double, farPlane: Double) {
            require(fovDegrees > 0.0 && fovDegrees < 180.0) { "Camera fov must be between 0 and 180 degrees" }
            require(nearPlane > 0.0) { "Camera near plane must be positive" }
            require(farPlane > nearPlane) { "Camera far plane must be greater than near plane" }
        }

        fun validatePitch(pitch: Double) {
            require(pitch in -89.0..89.0) { "Camera pitch must be between -89 and 89 degrees" }
        }
    }
}
