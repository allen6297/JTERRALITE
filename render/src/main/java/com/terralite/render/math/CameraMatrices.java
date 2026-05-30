package com.terralite.render.math;

import com.terralite.render.RenderCamera;
import com.terralite.render.Viewport;

/**
 * Converts a {@link RenderCamera} and {@link Viewport} into a combined view-projection
 * matrix for uploading to a shader uniform.
 *
 * <p>The camera looks along the -Z axis with +Y up. Rotation (yaw/pitch) will be
 * added when the engine Camera gains orientation state.
 */
public final class CameraMatrices {
    private CameraMatrices() {
    }

    /**
     * Returns a column-major float[16] MVP matrix (projection * view, model = identity).
     */
    public static float[] viewProjection(RenderCamera camera, Viewport viewport) {
        float fovRadians = (float) Math.toRadians(camera.fovDegrees());
        float aspect = (float) viewport.aspectRatio();
        float near = (float) camera.nearPlane();
        float far = (float) camera.farPlane();

        float eyeX = (float) camera.x();
        float eyeY = (float) camera.y();
        float eyeZ = (float) camera.z();

        Matrix4f projection = Matrix4f.perspective(fovRadians, aspect, near, far);
        Matrix4f view = Matrix4f.lookAt(
                eyeX, eyeY, eyeZ,
                eyeX, eyeY, eyeZ - 1.0f,   // look forward along -Z
                0.0f, 1.0f, 0.0f            // +Y up
        );

        return projection.multiply(view).toArray();
    }
}
