package com.terralite.engine.game;

import com.terralite.core.registry.RegistryManager;
import com.terralite.engine.input.InputState;
import com.terralite.engine.simulation.FixedTimestepSimulation;
import com.terralite.engine.world.World;

import java.util.Objects;

public record EngineContext(RegistryManager registries, World world, InputState input, FixedTimestepSimulation simulation) {
    public EngineContext {
        Objects.requireNonNull(registries, "registries");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(simulation, "simulation");
    }
}
