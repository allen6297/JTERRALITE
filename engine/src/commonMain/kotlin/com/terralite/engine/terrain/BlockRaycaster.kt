package com.terralite.engine.terrain

import com.terralite.engine.world.World

object BlockRaycaster {
    @JvmRecord
    data class HitResult(val blockPos: BlockPos, val adjacentPos: BlockPos) {
        /** Unit normal pointing from the hit block toward the adjacent (air) position. */
        fun normalX(): Int = adjacentPos.x - blockPos.x
        fun normalY(): Int = adjacentPos.y - blockPos.y
        fun normalZ(): Int = adjacentPos.z - blockPos.z
    }

    /**
     * DDA ray-march through world block storage. Returns the first occupied block
     * within [maxDistance], plus the adjacent air position (for placement).
     */
    @JvmStatic
    fun cast(
        world: World,
        originX: Double, originY: Double, originZ: Double,
        dirX: Double, dirY: Double, dirZ: Double,
        maxDistance: Double
    ): HitResult? {
        val len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ)
        if (len < 1e-9) return null

        val ndx = dirX / len
        val ndy = dirY / len
        val ndz = dirZ / len

        var bx = Math.floor(originX).toInt()
        var by = Math.floor(originY).toInt()
        var bz = Math.floor(originZ).toInt()

        val stepX = if (ndx > 0) 1 else -1
        val stepY = if (ndy > 0) 1 else -1
        val stepZ = if (ndz > 0) 1 else -1

        val tDeltaX = if (Math.abs(ndx) < 1e-9) Double.MAX_VALUE else Math.abs(1.0 / ndx)
        val tDeltaY = if (Math.abs(ndy) < 1e-9) Double.MAX_VALUE else Math.abs(1.0 / ndy)
        val tDeltaZ = if (Math.abs(ndz) < 1e-9) Double.MAX_VALUE else Math.abs(1.0 / ndz)

        var tMaxX = tDeltaX * if (stepX > 0) (bx + 1 - originX) else (originX - bx)
        var tMaxY = tDeltaY * if (stepY > 0) (by + 1 - originY) else (originY - by)
        var tMaxZ = tDeltaZ * if (stepZ > 0) (bz + 1 - originZ) else (originZ - bz)

        var prevX = bx; var prevY = by; var prevZ = bz

        while (true) {
            if (world.blocks().contains(BlockPos.of(bx, by, bz))) {
                return HitResult(BlockPos.of(bx, by, bz), BlockPos.of(prevX, prevY, prevZ))
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > maxDistance) break
                    prevX = bx; prevY = by; prevZ = bz
                    bx += stepX
                    tMaxX += tDeltaX
                } else {
                    if (tMaxZ > maxDistance) break
                    prevX = bx; prevY = by; prevZ = bz
                    bz += stepZ
                    tMaxZ += tDeltaZ
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > maxDistance) break
                    prevX = bx; prevY = by; prevZ = bz
                    by += stepY
                    tMaxY += tDeltaY
                } else {
                    if (tMaxZ > maxDistance) break
                    prevX = bx; prevY = by; prevZ = bz
                    bz += stepZ
                    tMaxZ += tDeltaZ
                }
            }
        }
        return null
    }
}
