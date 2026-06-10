package com.terralite.engine.save

import com.terralite.engine.chunk.ChunkPos

@JvmRecord
data class WorldSnapshot(
    val chunks: List<ChunkPos>,
    val entities: List<EntitySnapshot>,
    val blocks: List<BlockSnapshot>
) {
    constructor(chunks: List<ChunkPos>, entities: List<EntitySnapshot>) :
        this(chunks, entities, emptyList())
}
