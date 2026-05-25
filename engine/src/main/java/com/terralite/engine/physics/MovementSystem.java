package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

public final class MovementSystem implements WorldSimulationSystem {
    @Override
    public void tick(World world, SimulationTick tick) {
        double seconds = tick.delta().toNanos() / 1_000_000_000.0;

        for (Entity entity : world.entities().entities()) {
            if (!entity.has(PhysicsComponents.TRANSFORM) || !entity.has(PhysicsComponents.VELOCITY)) {
                continue;
            }

            Transform transform = entity.require(PhysicsComponents.TRANSFORM);
            Velocity velocity = entity.require(PhysicsComponents.VELOCITY);
            entity.set(PhysicsComponents.PREVIOUS_TRANSFORM, transform);
            entity.set(PhysicsComponents.TRANSFORM, transform.translate(
                velocity.x() * seconds,
                velocity.y() * seconds,
                velocity.z() * seconds
            ));
        }
    }
}
