package com.terralite.engine.camera;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.util.Objects;

public final class FollowCameraSystem implements WorldSimulationSystem {
    private final Camera camera;
    private final CameraTarget target;
    private final Transform offset;

    public FollowCameraSystem(Camera camera, CameraTarget target, Transform offset) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.target = Objects.requireNonNull(target, "target");
        this.offset = Objects.requireNonNull(offset, "offset");
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        Entity entity = world.entities().require(target.entityId());
        Transform targetTransform = entity.require(PhysicsComponents.TRANSFORM);
        camera.setTransform(targetTransform.translate(offset.x(), offset.y(), offset.z()));
    }
}
