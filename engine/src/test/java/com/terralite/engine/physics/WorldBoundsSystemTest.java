package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorldBoundsSystemTest {
    @Test
    void systemClampsEntitiesWithTransforms() {
        World world = new World();
        Entity entity = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, new Transform(12.0, -2.0, 5.0));

        new WorldBoundsSystem(new Bounds(0.0, 0.0, 0.0, 10.0, 10.0, 10.0))
            .tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(new Transform(10.0, 0.0, 5.0), entity.require(PhysicsComponents.TRANSFORM));
    }

    @Test
    void systemIgnoresEntitiesWithoutTransforms() {
        World world = new World();
        Entity entity = world.entities().create();

        new WorldBoundsSystem(new Bounds(0.0, 0.0, 0.0, 10.0, 10.0, 10.0))
            .tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertFalse(entity.has(PhysicsComponents.TRANSFORM));
    }
}
