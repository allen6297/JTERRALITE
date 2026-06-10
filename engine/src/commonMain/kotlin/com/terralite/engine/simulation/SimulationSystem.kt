package com.terralite.engine.simulation

fun interface SimulationSystem {
    fun tick(tick: SimulationTick)
}
