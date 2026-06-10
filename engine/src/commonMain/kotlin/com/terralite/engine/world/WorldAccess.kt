package com.terralite.engine.world

import com.terralite.engine.chunk.Chunk
import com.terralite.engine.chunk.ChunkPos

interface WorldAccess {
    fun getChunk(pos: ChunkPos): Chunk?
    fun requireChunk(pos: ChunkPos): Chunk
    fun containsChunk(pos: ChunkPos): Boolean
    fun chunkPositions(): Collection<ChunkPos>
}
