package com.terralite.engine.simulation;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WorldSimulationAdapterTest {
    @Test
    void adapterPassesWorldAndTickToWorldSystem() {
        World world = new World();
        SimulationTick tick = new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50));
        Entity[] created = new Entity[1];

        WorldSimulationAdapter adapter = new WorldSimulationAdapter(world, (tickWorld, simulationTick) -> {
            assertSame(world, tickWorld);
            assertEquals(tick, simulationTick);
            created[0] = tickWorld.entities().create();
        });

        adapter.tick(tick);

        assertSame(created[0], world.entities().require(created[0].id()));
    }
}
