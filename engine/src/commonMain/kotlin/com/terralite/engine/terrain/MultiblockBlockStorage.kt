package com.terralite.engine.terrain

class MultiblockBlockStorage(
    private val origins: BlockStorage,
    private val occupancy: (BlockState) -> List<BlockPos>
) : BlockStorage {
    private val occupiedOrigins: MutableMap<BlockPos, BlockPos> = LinkedHashMap()

    override fun get(pos: BlockPos): BlockState {
        if (origins.contains(pos)) return origins.get(pos)
        val origin = occupiedOrigins[pos] ?: return BlockState.AIR
        return origins.get(origin)
    }

    override fun set(pos: BlockPos, state: BlockState) {
        if (state.isAir()) {
            remove(pos)
            return
        }
        val existingOrigin = originOf(pos)
        if (existingOrigin != null) {
            if (existingOrigin != pos) {
                throw IllegalStateException(
                    "Cannot place ${state.id} at $pos; position belongs to $existingOrigin"
                )
            }
            remove(existingOrigin)
        }

        val offsets = occupancyFor(state)
        val occupiedPositions = offsets.map { offset -> add(pos, offset) }
        for (occupied in occupiedPositions) {
            val blockingOrigin = originOf(occupied)
            if (blockingOrigin != null) {
                throw IllegalStateException(
                    "Cannot place ${state.id} at $pos; occupied position $occupied belongs to $blockingOrigin"
                )
            }
        }

        origins.set(pos, state)
        for (occupied in occupiedPositions) {
            if (occupied != pos) {
                occupiedOrigins[occupied] = pos
            }
        }
    }

    override fun remove(pos: BlockPos): BlockState {
        val origin = originOf(pos) ?: return BlockState.AIR
        val removed = origins.remove(origin)
        for (occupied in occupiedPositions(origin, removed)) {
            occupiedOrigins.remove(occupied)
        }
        return removed
    }

    override fun stateId(pos: BlockPos): Int? {
        val origin = originOf(pos) ?: return null
        return origins.stateId(origin)
    }

    override fun contains(pos: BlockPos): Boolean =
        origins.contains(pos) || occupiedOrigins.containsKey(pos)

    override fun positions(): Collection<BlockPos> = origins.positions()

    override fun size(): Int = origins.size()

    fun isOrigin(pos: BlockPos): Boolean = origins.contains(pos)

    fun originOf(pos: BlockPos): BlockPos? {
        if (origins.contains(pos)) return pos
        return occupiedOrigins[pos]
    }

    fun occupiedPositions(origin: BlockPos): Collection<BlockPos> {
        val state = origins.get(origin)
        if (state.isAir()) return emptyList()
        return occupiedPositions(origin, state)
    }

    private fun occupiedPositions(origin: BlockPos, state: BlockState): List<BlockPos> =
        occupancyFor(state).map { offset -> add(origin, offset) }

    private fun occupancyFor(state: BlockState): List<BlockPos> {
        val offsets = occupancy(state)
        if (offsets.isEmpty()) return SINGLE_BLOCK
        return offsets.toList()
    }

    companion object {
        private val SINGLE_BLOCK = listOf(BlockPos.of(0, 0, 0))

        private fun add(pos: BlockPos, offset: BlockPos): BlockPos =
            BlockPos.of(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z)
    }
}
