package com.terralite.engine.game

import com.terralite.core.registry.RegistryManager
import com.terralite.engine.input.InputState
import com.terralite.engine.simulation.FixedTimestepSimulation
import com.terralite.engine.world.World

@JvmRecord
data class EngineContext(
    val registries: RegistryManager,
    val world: World,
    val input: InputState,
    val simulation: FixedTimestepSimulation
)
