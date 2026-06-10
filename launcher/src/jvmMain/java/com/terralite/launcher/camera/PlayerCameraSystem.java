package com.terralite.launcher.camera;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.util.Objects;

/**
 * Positions the camera relative to the player entity based on the current {@link CameraMode}.
 *
 * <ul>
 *   <li>First-person: camera at the player's eye position (y + 1.6).</li>
 *   <li>Third-person: camera pulled back behind and above the player.</li>
 * </ul>
 */
public final class PlayerCameraSystem implements WorldSimulationSystem {
    // feet = transform.y - colliderHalfHeight(0.9); eye at feet + 1.5 = transform.y + 0.6
    private static final double EYE_HEIGHT = 0.6;
    private static final double THIRD_PERSON_DISTANCE = 5.0;
    private static final double THIRD_PERSON_HEIGHT = 2.0;

    private final Camera camera;
    private final EntityId playerId;
    private CameraMode mode;

    public PlayerCameraSystem(Camera camera, EntityId playerId, CameraMode initialMode) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.mode = Objects.requireNonNull(initialMode, "initialMode");
    }

    public void setMode(CameraMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public CameraMode mode() {
        return mode;
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        Entity player = world.entities().require(playerId);
        Transform t = player.require(PhysicsComponents.TRANSFORM);

        if (mode == CameraMode.FIRST_PERSON) {
            camera.setTransform(new Transform(t.x(), t.y() + EYE_HEIGHT, t.z()));
        } else {
            double yawRad = Math.toRadians(camera.yaw());
            double backX = Math.sin(yawRad) * THIRD_PERSON_DISTANCE;
            double backZ = Math.cos(yawRad) * THIRD_PERSON_DISTANCE;
            camera.setTransform(new Transform(
                    t.x() + backX,
                    t.y() + EYE_HEIGHT + THIRD_PERSON_HEIGHT,
                    t.z() + backZ
            ));
        }
    }
}
