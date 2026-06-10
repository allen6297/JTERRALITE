package com.terralite.engine.simulation

import com.terralite.engine.world.World

fun interface WorldSimulationSystem {
    fun tick(world: World, tick: SimulationTick)
}
