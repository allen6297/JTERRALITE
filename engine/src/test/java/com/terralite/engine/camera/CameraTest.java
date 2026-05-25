package com.terralite.engine.camera;

import com.terralite.engine.physics.Transform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CameraTest {
    @Test
    void cameraStoresTransformAndProjection() {
        Camera camera = new Camera(new Transform(1.0, 2.0, 3.0), 75.0, 0.1, 500.0);

        assertEquals(new Transform(1.0, 2.0, 3.0), camera.transform());
        assertEquals(75.0, camera.fovDegrees());
        assertEquals(0.1, camera.nearPlane());
        assertEquals(500.0, camera.farPlane());
    }

    @Test
    void cameraUpdatesTransform() {
        Camera camera = new Camera();

        camera.setTransform(new Transform(4.0, 5.0, 6.0));

        assertEquals(new Transform(4.0, 5.0, 6.0), camera.transform());
    }

    @Test
    void cameraRejectsInvalidProjectionValues() {
        assertThrows(IllegalArgumentException.class, () -> new Camera(Transform.ORIGIN, 0.0, 0.1, 100.0));
        assertThrows(IllegalArgumentException.class, () -> new Camera(Transform.ORIGIN, 180.0, 0.1, 100.0));
        assertThrows(IllegalArgumentException.class, () -> new Camera(Transform.ORIGIN, 70.0, 0.0, 100.0));
        assertThrows(IllegalArgumentException.class, () -> new Camera(Transform.ORIGIN, 70.0, 10.0, 10.0));
    }
}
