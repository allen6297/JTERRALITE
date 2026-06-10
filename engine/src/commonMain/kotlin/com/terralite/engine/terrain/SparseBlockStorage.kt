package com.terralite.engine.terrain

class SparseBlockStorage : BlockStorage {
    private val blocks: MutableMap<BlockPos, BlockState> = LinkedHashMap()

    override fun get(pos: BlockPos): BlockState = blocks.getOrDefault(pos, BlockState.AIR)

    override fun set(pos: BlockPos, state: BlockState) {
        if (state.isAir()) {
            blocks.remove(pos)
        } else {
            blocks[pos] = state
        }
    }

    override fun remove(pos: BlockPos): BlockState = blocks.remove(pos) ?: BlockState.AIR

    override fun contains(pos: BlockPos): Boolean = blocks.containsKey(pos)

    override fun positions(): Collection<BlockPos> = blocks.keys.toList()

    override fun size(): Int = blocks.size
}
