package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.util.Objects;

public final class WorldBoundsSystem implements WorldSimulationSystem {
    private final Bounds bounds;

    public WorldBoundsSystem(Bounds bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        for (Entity entity : world.entities().entities()) {
            if (!entity.has(PhysicsComponents.TRANSFORM)) {
                continue;
            }

            Transform transform = entity.require(PhysicsComponents.TRANSFORM);
            entity.set(PhysicsComponents.TRANSFORM, bounds.clamp(transform));
        }
    }
}
