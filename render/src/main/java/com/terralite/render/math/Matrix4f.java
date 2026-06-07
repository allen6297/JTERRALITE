package com.terralite.render.math;

/**
 * Minimal 4x4 float matrix stored in column-major order for OpenGL uniform upload.
 * Index layout: m[col * 4 + row]
 */
public final class Matrix4f {
    private final float[] m;

    private Matrix4f(float[] m) {
        this.m = m;
    }

    public static Matrix4f identity() {
        float[] m = new float[16];
        m[0] = 1; m[5] = 1; m[10] = 1; m[15] = 1;
        return new Matrix4f(m);
    }

    /**
     * Vulkan perspective projection (Y-flipped, depth range 0..1).
     *
     * <p>Vulkan NDC has Y+ pointing down, so m[5] is negated relative to the
     * OpenGL convention. Depth is remapped to [0, 1].
     *
     * @param fovYRadians vertical field of view in radians
     * @param aspect      viewport width / height
     * @param near        near clip plane (positive)
     * @param far         far clip plane (positive, > near)
     */
    public static Matrix4f perspective(float fovYRadians, float aspect, float near, float far) {
        float f = 1.0f / (float) Math.tan(fovYRadians * 0.5f);
        float rangeInv = 1.0f / (near - far);

        float[] m = new float[16];
        m[0]  = f / aspect;
        m[5]  = -f; // negate Y: Vulkan NDC has Y+ pointing down
        m[10] = far * rangeInv;           // Vulkan depth [0, 1]
        m[11] = -1.0f;
        m[14] = far * near * rangeInv;
        return new Matrix4f(m);
    }

    /**
     * View matrix via lookAt — positions the camera at {@code eye} pointing toward {@code center}.
     */
    public static Matrix4f lookAt(
            float eyeX, float eyeY, float eyeZ,
            float centerX, float centerY, float centerZ,
            float upX, float upY, float upZ
    ) {
        // forward = normalize(center - eye)
        float fx = centerX - eyeX, fy = centerY - eyeY, fz = centerZ - eyeZ;
        float fLen = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fLen; fy /= fLen; fz /= fLen;

        // right = normalize(forward × up)
        float rx = fy * upZ - fz * upY;
        float ry = fz * upX - fx * upZ;
        float rz = fx * upY - fy * upX;
        float rLen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        rx /= rLen; ry /= rLen; rz /= rLen;

        // corrected up = right × forward
        float ux = ry * fz - rz * fy;
        float uy = rz * fx - rx * fz;
        float uz = rx * fy - ry * fx;

        float[] m = new float[16];
        // column 0
        m[0] = rx;  m[1] = ux;  m[2] = -fx; m[3] = 0;
        // column 1
        m[4] = ry;  m[5] = uy;  m[6] = -fy; m[7] = 0;
        // column 2
        m[8] = rz;  m[9] = uz;  m[10] = -fz; m[11] = 0;
        // column 3 — translation
        m[12] = -(rx * eyeX + ry * eyeY + rz * eyeZ);
        m[13] = -(ux * eyeX + uy * eyeY + uz * eyeZ);
        m[14] =  (fx * eyeX + fy * eyeY + fz * eyeZ);
        m[15] = 1;
        return new Matrix4f(m);
    }

    /**
     * Returns {@code this * other} as a new matrix.
     */
    public Matrix4f multiply(Matrix4f other) {
        float[] a = this.m;
        float[] b = other.m;
        float[] c = new float[16];

        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += a[k * 4 + row] * b[col * 4 + k];
                }
                c[col * 4 + row] = sum;
            }
        }
        return new Matrix4f(c);
    }

    /** Column-major float[16] suitable for {@code glUniformMatrix4fv}. */
    public float[] toArray() {
        return m.clone();
    }
}
