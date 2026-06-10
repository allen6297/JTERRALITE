package com.terralite.engine.physics

@JvmRecord
data class Collider(val halfWidth: Double, val halfHeight: Double, val halfDepth: Double) {
    init {
        require(halfWidth >= 0.0) { "Collider half width cannot be negative" }
        require(halfHeight >= 0.0) { "Collider half height cannot be negative" }
        require(halfDepth >= 0.0) { "Collider half depth cannot be negative" }
    }

    fun boundsAt(transform: Transform): Aabb = Aabb(
        transform.x - halfWidth,
        transform.y - halfHeight,
        transform.z - halfDepth,
        transform.x + halfWidth,
        transform.y + halfHeight,
        transform.z + halfDepth
    )
}
