package com.terralite.engine.player;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.input.InputState;
import com.terralite.engine.input.MovementIntent;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Velocity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.util.Objects;

public final class PlayerInputSystem implements WorldSimulationSystem {
    private final InputState input;

    public PlayerInputSystem(InputState input) {
        this.input = Objects.requireNonNull(input, "input");
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        MovementIntent intent = MovementIntent.from(input);

        for (Entity entity : world.entities().entities()) {
            if (!entity.has(PlayerComponents.PLAYER_CONTROLLED)) {
                continue;
            }

            PlayerControlled player = entity.require(PlayerComponents.PLAYER_CONTROLLED);
            entity.set(PhysicsComponents.VELOCITY, new Velocity(
                intent.x() * player.movementSpeed(),
                intent.y() * player.movementSpeed(),
                intent.z() * player.movementSpeed()
            ));
        }
    }
}
