package com.terralite.engine.physics

import com.terralite.engine.entity.Entity
import com.terralite.engine.terrain.BlockPos

@JvmRecord
data class BlockCollision(val entity: Entity, val blockPos: BlockPos, val blockBounds: Aabb)
