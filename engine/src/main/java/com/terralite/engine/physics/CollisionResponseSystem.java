package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.util.Objects;

public final class CollisionResponseSystem implements WorldSimulationSystem {
    private final CollisionDetector detector;

    public CollisionResponseSystem() {
        this(new CollisionDetector());
    }

    public CollisionResponseSystem(CollisionDetector detector) {
        this.detector = Objects.requireNonNull(detector, "detector");
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        for (CollisionPair collision : detector.detect(world)) {
            revertIfMoved(collision.first());
            revertIfMoved(collision.second());
        }
    }

    private static void revertIfMoved(Entity entity) {
        entity.get(PhysicsComponents.PREVIOUS_TRANSFORM).ifPresent(previous -> {
            entity.set(PhysicsComponents.TRANSFORM, previous);
            if (entity.has(PhysicsComponents.VELOCITY)) {
                entity.set(PhysicsComponents.VELOCITY, Velocity.ZERO);
            }
        });
    }
}
