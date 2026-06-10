package com.terralite.engine.chunk

@JvmRecord
data class Chunk(val pos: ChunkPos) {
    init {
        requireNotNull(pos) { "pos" }
    }
}
