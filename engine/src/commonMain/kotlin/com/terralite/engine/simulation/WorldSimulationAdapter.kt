package com.terralite.engine.simulation

import com.terralite.engine.world.World

class WorldSimulationAdapter(
    private val world: World,
    private val system: WorldSimulationSystem
) : SimulationSystem {
    override fun tick(tick: SimulationTick) {
        system.tick(world, tick)
    }
}
