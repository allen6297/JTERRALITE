package com.terralite.engine.chunk

interface ChunkStorage {
    fun get(pos: ChunkPos): Chunk?
    fun contains(pos: ChunkPos): Boolean
    fun put(chunk: Chunk): Chunk
    fun remove(pos: ChunkPos): Chunk
    fun require(pos: ChunkPos): Chunk
    fun positions(): Collection<ChunkPos>
    fun chunks(): Collection<Chunk>
    fun size(): Int
}
