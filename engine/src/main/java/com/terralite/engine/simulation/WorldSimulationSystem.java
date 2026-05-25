package com.terralite.engine.simulation;

import com.terralite.engine.world.World;

public interface WorldSimulationSystem {
    void tick(World world, SimulationTick tick);
}
