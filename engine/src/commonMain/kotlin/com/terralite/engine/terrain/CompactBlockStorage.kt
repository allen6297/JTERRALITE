package com.terralite.engine.terrain

class CompactBlockStorage(
    private val stateIds: (BlockState) -> Int,
    private val states: (Int) -> BlockState,
    private val airStateId: Int
) : BlockStorage {
    private val blocks: MutableMap<BlockPos, Int> = LinkedHashMap()

    override fun get(pos: BlockPos): BlockState {
        val stateId = blocks[pos] ?: return BlockState.AIR
        return states(stateId)
    }

    fun getStateId(pos: BlockPos): Int = blocks.getOrDefault(pos, airStateId)

    override fun stateId(pos: BlockPos): Int = getStateId(pos)

    override fun set(pos: BlockPos, state: BlockState) {
        setStateId(pos, stateIds(state))
    }

    fun setStateId(pos: BlockPos, stateId: Int) {
        if (stateId == airStateId || states(stateId).isAir()) {
            blocks.remove(pos)
        } else {
            blocks[pos] = stateId
        }
    }

    override fun remove(pos: BlockPos): BlockState {
        val removed = blocks.remove(pos) ?: return BlockState.AIR
        return states(removed)
    }

    fun removeStateId(pos: BlockPos): Int = blocks.remove(pos) ?: airStateId

    override fun contains(pos: BlockPos): Boolean = blocks.containsKey(pos)

    override fun positions(): Collection<BlockPos> = blocks.keys.toList()

    override fun size(): Int = blocks.size
}
