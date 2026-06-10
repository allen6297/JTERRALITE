package com.terralite.engine.terrain

import com.terralite.engine.chunk.ChunkPos
import com.terralite.engine.entity.EntityId
import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.physics.Transform
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class ChunkUnloaderSystem(
    private val target: EntityId,
    private val chunkSize: Int,
    private val radius: ChunkLoadRadius
) : WorldSimulationSystem {
    init {
        require(chunkSize > 0) { "Chunk size must be positive" }
    }

    override fun tick(world: World, tick: SimulationTick) {
        val entity = world.entities().require(target)
        val transform = entity.require(PhysicsComponents.TRANSFORM)
        val center = chunkPosFor(transform)

        for (pos in world.chunkPositions().toList()) {
            if (!isWithinRadius(pos, center)) {
                world.removeChunk(pos)
            }
        }
    }

    fun isWithinRadius(pos: ChunkPos, center: ChunkPos): Boolean =
        Math.abs(pos.x - center.x) <= radius.horizontal &&
        Math.abs(pos.z - center.z) <= radius.horizontal &&
        Math.abs(pos.y - center.y) <= radius.vertical

    fun chunkPosFor(transform: Transform): ChunkPos = ChunkPos.of(
        floorDiv(transform.x, chunkSize),
        floorDiv(transform.y, chunkSize),
        floorDiv(transform.z, chunkSize)
    )

    private companion object {
        fun floorDiv(value: Double, divisor: Int): Int = Math.floor(value / divisor).toInt()
    }
}
