package com.terralite.engine.save

import com.terralite.engine.terrain.BlockPos
import com.terralite.engine.terrain.BlockState

@JvmRecord
data class BlockSnapshot(val pos: BlockPos, val state: BlockState, val stateId: Int?)
