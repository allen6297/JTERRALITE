package com.terralite.engine.weather

import com.terralite.engine.simulation.SimulationSystem
import com.terralite.engine.simulation.SimulationTick

/**
 * Cycles through weather types at a fixed interval. All durations are in nanoseconds.
 */
class WeatherCycleSystem(
    private val weather: WeatherState,
    private val intervalNanos: Long,
    private val cycle: List<WeatherType>
) : SimulationSystem {
    private var elapsedNanos: Long = 0L
    private var index: Int = 0

    init {
        require(intervalNanos > 0) { "Weather interval must be positive" }
        require(cycle.isNotEmpty()) { "Weather cycle cannot be empty" }
        weather.set(cycle.first(), intensityFor(cycle.first()))
    }

    override fun tick(tick: SimulationTick) {
        elapsedNanos += tick.delta
        while (elapsedNanos >= intervalNanos) {
            elapsedNanos -= intervalNanos
            index = (index + 1) % cycle.size
            val next = cycle[index]
            weather.set(next, intensityFor(next))
        }
    }

    companion object {
        @JvmStatic fun defaultCycle(weather: WeatherState, intervalNanos: Long): WeatherCycleSystem =
            WeatherCycleSystem(weather, intervalNanos, listOf(WeatherType.CLEAR, WeatherType.RAIN, WeatherType.STORM))

        private fun intensityFor(type: WeatherType): Double = when (type) {
            WeatherType.CLEAR -> 0.0
            WeatherType.RAIN -> 0.6
            WeatherType.STORM -> 1.0
        }
    }
}
