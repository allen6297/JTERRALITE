package com.terralite.engine.terrain

import com.terralite.engine.chunk.Chunk
import com.terralite.engine.chunk.ChunkPos
import com.terralite.engine.entity.EntityId
import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.physics.Transform
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class ChunkLoaderSystem(
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

        for (y in center.y - radius.vertical..center.y + radius.vertical) {
            for (z in center.z - radius.horizontal..center.z + radius.horizontal) {
                for (x in center.x - radius.horizontal..center.x + radius.horizontal) {
                    val pos = ChunkPos.of(x, y, z)
                    if (!world.containsChunk(pos)) {
                        world.putChunk(Chunk(pos))
                    }
                }
            }
        }
    }

    fun chunkPosFor(transform: Transform): ChunkPos = ChunkPos.of(
        floorDiv(transform.x, chunkSize),
        floorDiv(transform.y, chunkSize),
        floorDiv(transform.z, chunkSize)
    )

    private companion object {
        fun floorDiv(value: Double, divisor: Int): Int = Math.floor(value / divisor).toInt()
    }
}
