package com.terralite.engine.terrain

interface BlockStorage {
    fun get(pos: BlockPos): BlockState
    fun set(pos: BlockPos, state: BlockState)
    fun remove(pos: BlockPos): BlockState
    fun stateId(pos: BlockPos): Int? = null
    fun contains(pos: BlockPos): Boolean
    fun positions(): Collection<BlockPos>
    fun size(): Int
}
