package com.terralite.engine.physics

import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class WorldBoundsSystem(private val bounds: Bounds) : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        for (entity in world.entities().entities()) {
            if (!entity.has(PhysicsComponents.TRANSFORM)) continue
            val transform = entity.require(PhysicsComponents.TRANSFORM)
            entity.set(PhysicsComponents.TRANSFORM, bounds.clamp(transform))
        }
    }
}
