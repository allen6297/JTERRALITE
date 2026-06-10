package com.terralite.engine.time

import com.terralite.engine.simulation.SimulationSystem
import com.terralite.engine.simulation.SimulationTick

class GameClockSystem(private val clock: GameClock) : SimulationSystem {
    override fun tick(tick: SimulationTick) {
        clock.advance(tick.delta)
    }
}
