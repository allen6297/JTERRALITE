package com.terralite.engine.physics

import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class GravitySystem : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        val dt = tick.delta / 1_000_000_000.0

        for (entity in world.entities().entities()) {
            if (!entity.has(PhysicsComponents.VELOCITY)) continue
            val grounded = entity.get(PhysicsComponents.GROUNDED) ?: false
            if (grounded) continue
            val v = entity.require(PhysicsComponents.VELOCITY)
            val newVy = maxOf(TERMINAL_VELOCITY, v.y + GRAVITY * dt)
            entity.set(PhysicsComponents.VELOCITY, Velocity(v.x, newVy, v.z))
        }
    }

    companion object {
        private const val GRAVITY = -20.0
        private const val TERMINAL_VELOCITY = -50.0
    }
}
