package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

public final class GravitySystem implements WorldSimulationSystem {
    private static final double GRAVITY = -20.0;
    private static final double TERMINAL_VELOCITY = -50.0;

    @Override
    public void tick(World world, SimulationTick tick) {
        double dt = tick.delta().toNanos() / 1_000_000_000.0;

        for (Entity entity : world.entities().entities()) {
            if (!entity.has(PhysicsComponents.VELOCITY)) continue;

            boolean grounded = entity.get(PhysicsComponents.GROUNDED).orElse(false);
            if (grounded) continue;

            Velocity v = entity.require(PhysicsComponents.VELOCITY);
            double newVy = Math.max(TERMINAL_VELOCITY, v.y() + GRAVITY * dt);
            entity.set(PhysicsComponents.VELOCITY, new Velocity(v.x(), newVy, v.z()));
        }
    }
}
