package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollisionResponseSystemTest {
    @Test
    void responseRevertsMovedEntityAndStopsVelocityWhenCollidingWithStaticEntity() {
        World world = new World();
        Entity mover = world.entities().create()
            .set(PhysicsComponents.PREVIOUS_TRANSFORM, new Transform(-2.0, 0.0, 0.0))
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN)
            .set(PhysicsComponents.VELOCITY, new Velocity(2.0, 0.0, 0.0))
            .set(PhysicsComponents.COLLIDER, new Collider(0.5, 0.5, 0.5));
        Entity wall = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN)
            .set(PhysicsComponents.COLLIDER, new Collider(0.5, 0.5, 0.5));

        new CollisionResponseSystem().tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(new Transform(-2.0, 0.0, 0.0), mover.require(PhysicsComponents.TRANSFORM));
        assertEquals(Velocity.ZERO, mover.require(PhysicsComponents.VELOCITY));
        assertEquals(Transform.ORIGIN, wall.require(PhysicsComponents.TRANSFORM));
    }

    @Test
    void responseRevertsBothMovedEntitiesInCollisionPair() {
        World world = new World();
        Entity first = world.entities().create()
            .set(PhysicsComponents.PREVIOUS_TRANSFORM, new Transform(-2.0, 0.0, 0.0))
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN)
            .set(PhysicsComponents.VELOCITY, new Velocity(1.0, 0.0, 0.0))
            .set(PhysicsComponents.COLLIDER, new Collider(0.5, 0.5, 0.5));
        Entity second = world.entities().create()
            .set(PhysicsComponents.PREVIOUS_TRANSFORM, new Transform(2.0, 0.0, 0.0))
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN)
            .set(PhysicsComponents.VELOCITY, new Velocity(-1.0, 0.0, 0.0))
            .set(PhysicsComponents.COLLIDER, new Collider(0.5, 0.5, 0.5));

        new CollisionResponseSystem().tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(new Transform(-2.0, 0.0, 0.0), first.require(PhysicsComponents.TRANSFORM));
        assertEquals(new Transform(2.0, 0.0, 0.0), second.require(PhysicsComponents.TRANSFORM));
        assertEquals(Velocity.ZERO, first.require(PhysicsComponents.VELOCITY));
        assertEquals(Velocity.ZERO, second.require(PhysicsComponents.VELOCITY));
    }
}
