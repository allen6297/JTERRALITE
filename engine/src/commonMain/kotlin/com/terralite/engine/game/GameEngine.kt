package com.terralite.engine.game

import com.terralite.core.registry.RegistryManager
import com.terralite.engine.input.InputState
import com.terralite.engine.simulation.FixedTimestepSimulation
import com.terralite.engine.simulation.SimulationSystem
import com.terralite.engine.simulation.WorldSimulationAdapter
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class GameEngine private constructor(
    private val context: EngineContext,
    private val systems: List<EngineSystem>
) {
    private var state: EngineState = EngineState.CREATED

    init {
        for (system in systems) system.initialize(context)
    }

    fun context(): EngineContext = context
    fun state(): EngineState = state

    fun start() {
        if (state == EngineState.RUNNING) return
        if (state == EngineState.STOPPED) throw IllegalStateException("Engine cannot be restarted after stop")
        state = EngineState.RUNNING
        for (system in systems) system.start(context)
    }

    /** Advance the simulation by [nanos] nanoseconds. Returns the number of ticks run. */
    fun advance(nanos: Long): Int {
        ensureRunning()
        return context.simulation.advance(nanos)
    }

    fun stop() {
        if (state != EngineState.RUNNING) return
        for (i in systems.indices.reversed()) systems[i].stop(context)
        state = EngineState.STOPPED
    }

    private fun ensureRunning() {
        if (state != EngineState.RUNNING) throw IllegalStateException("Engine is not running")
    }

    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private val registries = RegistryManager()
        private val engineSystems: MutableList<EngineSystem> = mutableListOf()
        private val simulationSystems: MutableList<SimulationSystem> = mutableListOf()
        private val worldSimulationSystems: MutableList<WorldSimulationSystem> = mutableListOf()
        private var world: World = World()
        private var input: InputState = InputState()
        private var tickDeltaNanos: Long = 50_000_000L
        private var maxTicksPerAdvance: Int = 5

        fun tickDeltaMillis(millis: Long): Builder { tickDeltaNanos = millis * 1_000_000L; return this }
        fun tickDeltaNanos(nanos: Long): Builder { tickDeltaNanos = nanos; return this }
        fun maxTicksPerAdvance(max: Int): Builder { maxTicksPerAdvance = max; return this }
        fun addSystem(system: EngineSystem): Builder { engineSystems += system; return this }
        fun addSimulationSystem(system: SimulationSystem): Builder { simulationSystems += system; return this }
        fun addWorldSimulationSystem(system: WorldSimulationSystem): Builder { worldSimulationSystems += system; return this }
        fun world(world: World): Builder { this.world = world; return this }
        fun input(input: InputState): Builder { this.input = input; return this }

        fun build(): GameEngine {
            val allSystems: MutableList<SimulationSystem> = simulationSystems.toMutableList()
            for (system in worldSimulationSystems) {
                allSystems += WorldSimulationAdapter(world, system)
            }
            val simulation = FixedTimestepSimulation.builder()
                .tickDeltaNanos(tickDeltaNanos)
                .maxTicksPerAdvance(maxTicksPerAdvance)
                .addSystems(allSystems)
                .build()
            return GameEngine(EngineContext(registries, world, input, simulation), engineSystems.toList())
        }
    }
}
