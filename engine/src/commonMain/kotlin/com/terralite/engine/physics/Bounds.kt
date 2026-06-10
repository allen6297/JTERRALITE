package com.terralite.engine.physics

@JvmRecord
data class Bounds(
    val minX: Double, val minY: Double, val minZ: Double,
    val maxX: Double, val maxY: Double, val maxZ: Double
) {
    init {
        require(maxX >= minX) { "Max x cannot be less than min x" }
        require(maxY >= minY) { "Max y cannot be less than min y" }
        require(maxZ >= minZ) { "Max z cannot be less than min z" }
    }

    fun clamp(transform: Transform): Transform = Transform(
        transform.x.coerceIn(minX, maxX),
        transform.y.coerceIn(minY, maxY),
        transform.z.coerceIn(minZ, maxZ)
    )

    fun contains(transform: Transform): Boolean =
        transform.x in minX..maxX &&
        transform.y in minY..maxY &&
        transform.z in minZ..maxZ
}
