package com.terralite.engine.chunk

@JvmRecord
data class ChunkPos(val x: Int, val y: Int, val z: Int) {
    companion object {
        @JvmStatic fun of(x: Int, y: Int, z: Int): ChunkPos = ChunkPos(x, y, z)
    }
}
