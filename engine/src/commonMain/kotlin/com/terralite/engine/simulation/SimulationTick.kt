package com.terralite.engine.simulation

/**
 * Represents a single simulation step.
 *
 * @param index     1-based tick counter.
 * @param delta     Duration of this tick in nanoseconds. Must be positive.
 * @param totalTime Accumulated simulation time in nanoseconds since start. Must be non-negative.
 */
@JvmRecord
data class SimulationTick(val index: Long, val delta: Long, val totalTime: Long) {
    init {
        require(index > 0) { "Tick index must be positive" }
        require(delta > 0) { "Tick delta must be positive" }
        require(totalTime >= 0) { "Total time cannot be negative" }
    }
}
