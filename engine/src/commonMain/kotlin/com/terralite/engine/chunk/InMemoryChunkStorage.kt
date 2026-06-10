package com.terralite.engine.chunk

class InMemoryChunkStorage : ChunkStorage {
    private val chunks: MutableMap<ChunkPos, Chunk> = LinkedHashMap()

    override fun get(pos: ChunkPos): Chunk? = chunks[pos]

    override fun contains(pos: ChunkPos): Boolean = chunks.containsKey(pos)

    override fun put(chunk: Chunk): Chunk {
        chunks[chunk.pos] = chunk
        return chunk
    }

    override fun remove(pos: ChunkPos): Chunk {
        return chunks.remove(pos) ?: throw IllegalArgumentException("Missing chunk: $pos")
    }

    override fun require(pos: ChunkPos): Chunk {
        return get(pos) ?: throw IllegalArgumentException("Missing chunk: $pos")
    }

    override fun positions(): Collection<ChunkPos> = chunks.keys.toList()

    override fun chunks(): Collection<Chunk> = chunks.values.toList()

    override fun size(): Int = chunks.size
}
