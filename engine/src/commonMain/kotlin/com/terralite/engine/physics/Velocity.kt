package com.terralite.engine.physics

@JvmRecord
data class Velocity(val x: Double, val y: Double, val z: Double) {
    companion object {
        @JvmField val ZERO = Velocity(0.0, 0.0, 0.0)
    }

    fun scale(scalar: Double): Velocity = Velocity(x * scalar, y * scalar, z * scalar)
}
