package com.terralite.engine.player;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.input.InputActions;
import com.terralite.engine.input.InputState;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Velocity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.util.Objects;

/**
 * Converts WASD input into world-space velocity based on the camera's current yaw,
 * so the player always moves in the direction they are facing.
 */
public final class YawRelativePlayerInputSystem implements WorldSimulationSystem {
    private static final double SPRINT_MULTIPLIER = 2.0;
    private static final double JUMP_SPEED = 8.0;

    private final InputState input;
    private final Camera camera;

    public YawRelativePlayerInputSystem(InputState input, Camera camera) {
        this.input = Objects.requireNonNull(input, "input");
        this.camera = Objects.requireNonNull(camera, "camera");
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        double yawRad = Math.toRadians(camera.yaw());

        // Camera forward is (-sin(yaw), 0, -cos(yaw)); right is (cos(yaw), 0, -sin(yaw))
        double fwdX = -Math.sin(yawRad);
        double fwdZ = -Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        double forward = 0.0;
        double strafe = 0.0;
        if (input.isPressed(InputActions.MOVE_FORWARD)) forward += 1.0;
        if (input.isPressed(InputActions.MOVE_BACK))    forward -= 1.0;
        if (input.isPressed(InputActions.MOVE_RIGHT))   strafe  += 1.0;
        if (input.isPressed(InputActions.MOVE_LEFT))    strafe  -= 1.0;

        double worldDx = forward * fwdX + strafe * rightX;
        double worldDz = forward * fwdZ + strafe * rightZ;

        // Normalise diagonal movement
        double horizLen = Math.sqrt(worldDx * worldDx + worldDz * worldDz);
        if (horizLen > 1.0) {
            worldDx /= horizLen;
            worldDz /= horizLen;
        }

        for (Entity entity : world.entities().entities()) {
            if (!entity.has(PlayerComponents.PLAYER_CONTROLLED)) {
                continue;
            }
            PlayerControlled player = entity.require(PlayerComponents.PLAYER_CONTROLLED);
            double speed = player.movementSpeed();
            if (input.isPressed(InputActions.SPRINT)) {
                speed *= SPRINT_MULTIPLIER;
            }

            double currentVy = entity.has(PhysicsComponents.VELOCITY)
                    ? entity.require(PhysicsComponents.VELOCITY).y()
                    : 0.0;
            boolean grounded = entity.get(PhysicsComponents.GROUNDED).orElse(false);
            double vy = (input.isPressed(InputActions.JUMP) && grounded) ? JUMP_SPEED : currentVy;

            entity.set(PhysicsComponents.VELOCITY, new Velocity(
                    worldDx * speed,
                    vy,
                    worldDz * speed
            ));
        }
    }
}
