package com.terralite.engine.simulation;

import com.terralite.engine.world.World;

import java.util.Objects;

public final class WorldSimulationAdapter implements SimulationSystem {
    private final World world;
    private final WorldSimulationSystem system;

    public WorldSimulationAdapter(World world, WorldSimulationSystem system) {
        this.world = Objects.requireNonNull(world, "world");
        this.system = Objects.requireNonNull(system, "system");
    }

    @Override
    public void tick(SimulationTick tick) {
        system.tick(world, tick);
    }
}
