package com.terralite.engine.physics

import com.terralite.engine.entity.Entity
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class CollisionResponseSystem @JvmOverloads constructor(
    private val detector: CollisionDetector = CollisionDetector()
) : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        for (collision in detector.detect(world)) {
            revertIfMoved(collision.first)
            revertIfMoved(collision.second)
        }
    }

    private fun revertIfMoved(entity: Entity) {
        val previous = entity.get(PhysicsComponents.PREVIOUS_TRANSFORM) ?: return
        entity.set(PhysicsComponents.TRANSFORM, previous)
        if (entity.has(PhysicsComponents.VELOCITY)) {
            entity.set(PhysicsComponents.VELOCITY, Velocity.ZERO)
        }
    }
}
