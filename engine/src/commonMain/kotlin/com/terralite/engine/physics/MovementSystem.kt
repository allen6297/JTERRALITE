package com.terralite.engine.physics

import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class MovementSystem : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        val seconds = tick.delta / 1_000_000_000.0

        for (entity in world.entities().entities()) {
            if (!entity.has(PhysicsComponents.TRANSFORM) || !entity.has(PhysicsComponents.VELOCITY)) continue
            val transform = entity.require(PhysicsComponents.TRANSFORM)
            val velocity = entity.require(PhysicsComponents.VELOCITY)
            entity.set(PhysicsComponents.PREVIOUS_TRANSFORM, transform)
            entity.set(PhysicsComponents.TRANSFORM, transform.translate(
                velocity.x * seconds,
                velocity.y * seconds,
                velocity.z * seconds
            ))
        }
    }
}
