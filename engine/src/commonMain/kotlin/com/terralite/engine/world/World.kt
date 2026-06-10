package com.terralite.engine.world

import com.terralite.engine.chunk.Chunk
import com.terralite.engine.chunk.ChunkPos
import com.terralite.engine.chunk.ChunkStorage
import com.terralite.engine.chunk.InMemoryChunkStorage
import com.terralite.engine.entity.EntityManager
import com.terralite.engine.entity.EntitySpawner
import com.terralite.engine.entity.InMemoryEntityManager
import com.terralite.engine.physics.Aabb
import com.terralite.engine.terrain.BlockPos
import com.terralite.engine.terrain.BlockState
import com.terralite.engine.terrain.BlockStorage
import com.terralite.engine.terrain.MultiblockBlockStorage
import com.terralite.engine.terrain.SparseBlockStorage

class World(
    private val chunks: ChunkStorage,
    private val entities: EntityManager,
    private val blocks: BlockStorage
) : WorldAccess {
    constructor() : this(InMemoryChunkStorage(), InMemoryEntityManager(), SparseBlockStorage())
    constructor(blocks: BlockStorage) : this(InMemoryChunkStorage(), InMemoryEntityManager(), blocks)
    constructor(chunks: ChunkStorage) : this(chunks, InMemoryEntityManager(), SparseBlockStorage())
    constructor(chunks: ChunkStorage, entities: EntityManager) : this(chunks, entities, SparseBlockStorage())

    fun chunks(): ChunkStorage = chunks
    fun entities(): EntityManager = entities
    fun blocks(): BlockStorage = blocks
    fun spawner(): EntitySpawner = EntitySpawner(this)

    fun getBlock(pos: BlockPos): BlockState = blocks.get(pos)
    fun setBlock(pos: BlockPos, state: BlockState) = blocks.set(pos, state)
    fun removeBlock(pos: BlockPos): BlockState = blocks.remove(pos)

    fun collisionBlockPositions(): Collection<BlockPos> {
        if (blocks is MultiblockBlockStorage) {
            val positions = mutableListOf<BlockPos>()
            for (origin in blocks.positions()) {
                positions += blocks.occupiedPositions(origin)
            }
            return positions.toList()
        }
        return blocks.positions()
    }

    fun blockCollisionBoxes(): Collection<Aabb> =
        collisionBlockPositions().map { pos ->
            Aabb(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                 pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)
        }

    fun putChunk(chunk: Chunk): Chunk = chunks.put(chunk)
    fun removeChunk(pos: ChunkPos): Chunk = chunks.remove(pos)

    override fun getChunk(pos: ChunkPos): Chunk? = chunks.get(pos)
    override fun requireChunk(pos: ChunkPos): Chunk = chunks.require(pos)
    override fun containsChunk(pos: ChunkPos): Boolean = chunks.contains(pos)
    override fun chunkPositions(): Collection<ChunkPos> = chunks.positions()
}
