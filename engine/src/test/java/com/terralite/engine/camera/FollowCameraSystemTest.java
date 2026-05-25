package com.terralite.engine.camera;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FollowCameraSystemTest {
    @Test
    void followCameraTracksTargetTransformWithOffset() {
        World world = new World();
        Entity target = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, new Transform(10.0, 2.0, -4.0));
        Camera camera = new Camera();
        FollowCameraSystem system = new FollowCameraSystem(
            camera,
            new CameraTarget(target.id()),
            new Transform(0.0, 5.0, -10.0)
        );

        system.tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(new Transform(10.0, 7.0, -14.0), camera.transform());
    }

    @Test
    void followCameraRequiresTargetTransform() {
        World world = new World();
        Entity target = world.entities().create();
        FollowCameraSystem system = new FollowCameraSystem(new Camera(), new CameraTarget(target.id()), Transform.ORIGIN);

        assertThrows(IllegalArgumentException.class,
            () -> system.tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50))));
    }
}
