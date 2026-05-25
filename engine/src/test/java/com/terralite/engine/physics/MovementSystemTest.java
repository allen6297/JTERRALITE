package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MovementSystemTest {
    @Test
    void movementSystemMovesEntitiesUsingVelocityAndTickDelta() {
        World world = new World();
        Entity entity = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN)
            .set(PhysicsComponents.VELOCITY, new Velocity(2.0, -4.0, 1.0));

        new MovementSystem().tick(world, new SimulationTick(1, Duration.ofMillis(500), Duration.ofMillis(500)));

        assertEquals(Transform.ORIGIN, entity.require(PhysicsComponents.PREVIOUS_TRANSFORM));
        assertEquals(new Transform(1.0, -2.0, 0.5), entity.require(PhysicsComponents.TRANSFORM));
    }

    @Test
    void movementSystemIgnoresEntitiesMissingRequiredComponents() {
        World world = new World();
        Entity onlyTransform = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);
        Entity onlyVelocity = world.entities().create()
            .set(PhysicsComponents.VELOCITY, new Velocity(1.0, 0.0, 0.0));
        Entity neither = world.entities().create();

        new MovementSystem().tick(world, new SimulationTick(1, Duration.ofSeconds(1), Duration.ofSeconds(1)));

        assertEquals(Transform.ORIGIN, onlyTransform.require(PhysicsComponents.TRANSFORM));
        assertFalse(onlyVelocity.has(PhysicsComponents.TRANSFORM));
        assertFalse(neither.has(PhysicsComponents.TRANSFORM));
    }
}
