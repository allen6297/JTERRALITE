package com.terralite.engine.physics

@JvmRecord
data class Aabb(
    val minX: Double, val minY: Double, val minZ: Double,
    val maxX: Double, val maxY: Double, val maxZ: Double
) {
    init {
        require(maxX >= minX) { "Max x cannot be less than min x" }
        require(maxY >= minY) { "Max y cannot be less than min y" }
        require(maxZ >= minZ) { "Max z cannot be less than min z" }
    }

    fun intersects(other: Aabb): Boolean =
        minX <= other.maxX && maxX >= other.minX &&
        minY <= other.maxY && maxY >= other.minY &&
        minZ <= other.maxZ && maxZ >= other.minZ
}
