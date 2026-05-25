package com.terralite.engine.player;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.input.InputActions;
import com.terralite.engine.input.InputState;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Velocity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlayerInputSystemTest {
    @Test
    void inputSystemWritesVelocityForPlayerControlledEntities() {
        World world = new World();
        InputState input = new InputState();
        Entity player = world.entities().create()
            .set(PlayerComponents.PLAYER_CONTROLLED, new PlayerControlled(4.0));

        input.press(InputActions.MOVE_FORWARD);
        input.press(InputActions.MOVE_RIGHT);
        input.press(InputActions.JUMP);

        new PlayerInputSystem(input).tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(new Velocity(4.0, 4.0, 4.0), player.require(PhysicsComponents.VELOCITY));
    }

    @Test
    void inputSystemIgnoresEntitiesWithoutPlayerControl() {
        World world = new World();
        InputState input = new InputState();
        Entity entity = world.entities().create();

        input.press(InputActions.MOVE_FORWARD);

        new PlayerInputSystem(input).tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertFalse(entity.has(PhysicsComponents.VELOCITY));
    }

    @Test
    void releasingInputResetsPlayerVelocityToZero() {
        World world = new World();
        InputState input = new InputState();
        Entity player = world.entities().create()
            .set(PlayerComponents.PLAYER_CONTROLLED, new PlayerControlled(4.0));
        PlayerInputSystem system = new PlayerInputSystem(input);

        input.press(InputActions.MOVE_FORWARD);
        system.tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        input.clear();
        system.tick(world, new SimulationTick(2, Duration.ofMillis(50), Duration.ofMillis(100)));

        assertEquals(Velocity.ZERO, player.require(PhysicsComponents.VELOCITY));
    }
}
