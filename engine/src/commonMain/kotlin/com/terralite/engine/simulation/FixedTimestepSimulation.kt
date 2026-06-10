package com.terralite.engine.simulation

/**
 * Runs a fixed-timestep simulation loop. All time values are in nanoseconds.
 */
class FixedTimestepSimulation private constructor(
    private val tickDeltaNanos: Long,
    private val maxTicksPerAdvance: Int,
    private val systems: List<SimulationSystem>
) {
    private var accumulatorNanos: Long = 0L
    private var tick: Long = 0L
    private var totalTimeNanos: Long = 0L

    fun tickDelta(): Long = tickDeltaNanos
    fun tick(): Long = tick
    fun totalTime(): Long = totalTimeNanos
    fun accumulator(): Long = accumulatorNanos

    fun advance(elapsedNanos: Long): Int {
        require(elapsedNanos >= 0) { "Elapsed time cannot be negative" }

        accumulatorNanos += elapsedNanos

        var ticksRun = 0
        while (accumulatorNanos >= tickDeltaNanos && ticksRun < maxTicksPerAdvance) {
            tick++
            totalTimeNanos += tickDeltaNanos

            val simulationTick = SimulationTick(tick, tickDeltaNanos, totalTimeNanos)
            for (system in systems) {
                system.tick(simulationTick)
            }

            accumulatorNanos -= tickDeltaNanos
            ticksRun++
        }

        if (ticksRun == maxTicksPerAdvance && accumulatorNanos >= tickDeltaNanos) {
            accumulatorNanos = tickDeltaNanos - 1
        }

        return ticksRun
    }

    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private val systems: MutableList<SimulationSystem> = mutableListOf()
        private var tickDeltaNanos: Long = 50_000_000L  // 50 ms
        private var maxTicksPerAdvance: Int = 5

        fun tickDeltaNanos(nanos: Long): Builder {
            require(nanos > 0) { "Tick delta must be positive" }
            tickDeltaNanos = nanos
            return this
        }

        fun tickDeltaMillis(millis: Long): Builder = tickDeltaNanos(millis * 1_000_000L)

        fun maxTicksPerAdvance(max: Int): Builder {
            require(max > 0) { "Max ticks per advance must be positive" }
            maxTicksPerAdvance = max
            return this
        }

        fun addSystem(system: SimulationSystem): Builder {
            systems += system
            return this
        }

        fun addSystems(list: List<SimulationSystem>): Builder {
            systems += list
            return this
        }

        fun build(): FixedTimestepSimulation = FixedTimestepSimulation(tickDeltaNanos, maxTicksPerAdvance, systems.toList())
    }
}
