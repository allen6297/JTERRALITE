package com.terralite.engine.terrain

@JvmRecord
data class ChunkLoadRadius(val horizontal: Int, val vertical: Int) {
    init {
        require(horizontal >= 0) { "Horizontal chunk load radius cannot be negative" }
        require(vertical >= 0) { "Vertical chunk load radius cannot be negative" }
    }

    companion object {
        @JvmStatic fun horizontal(radius: Int): ChunkLoadRadius = ChunkLoadRadius(radius, 0)
        @JvmStatic fun cubic(radius: Int): ChunkLoadRadius = ChunkLoadRadius(radius, radius)
    }
}
