package com.terralite.engine.physics

@JvmRecord
data class Transform(val x: Double, val y: Double, val z: Double) {
    companion object {
        @JvmField val ORIGIN = Transform(0.0, 0.0, 0.0)
    }

    fun translate(dx: Double, dy: Double, dz: Double): Transform =
        Transform(x + dx, y + dy, z + dz)
}
