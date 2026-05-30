package com.terralite.render.math;

import com.terralite.render.RenderCamera;
import com.terralite.render.Viewport;

/**
 * Converts a {@link RenderCamera} and {@link Viewport} into a combined view-projection
 * matrix for uploading to a shader uniform.
 *
 * <p>Forward direction is derived from {@link RenderCamera#yaw()} and
 * {@link RenderCamera#pitch()}:
 * <ul>
 *   <li>yaw=0, pitch=0 → looking along -Z with +Y up</li>
 *   <li>yaw increases counterclockwise around Y (right-hand)</li>
 *   <li>positive pitch looks upward</li>
 * </ul>
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

        // Compute forward direction from yaw + pitch
        double yawRad = Math.toRadians(camera.yaw());
        double pitchRad = Math.toRadians(camera.pitch());
        float forwardX = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
        float forwardY = (float) Math.sin(pitchRad);
        float forwardZ = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));

        Matrix4f projection = Matrix4f.perspective(fovRadians, aspect, near, far);
        Matrix4f view = Matrix4f.lookAt(
                eyeX, eyeY, eyeZ,
                eyeX + forwardX, eyeY + forwardY, eyeZ + forwardZ,
                0.0f, 1.0f, 0.0f
        );

        return projection.multiply(view).toArray();
    }
}
