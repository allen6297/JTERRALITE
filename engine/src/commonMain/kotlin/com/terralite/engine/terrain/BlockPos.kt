package com.terralite.engine.terrain

@JvmRecord
data class BlockPos(val x: Int, val y: Int, val z: Int) {
    companion object {
        @JvmStatic fun of(x: Int, y: Int, z: Int): BlockPos = BlockPos(x, y, z)
    }
}
