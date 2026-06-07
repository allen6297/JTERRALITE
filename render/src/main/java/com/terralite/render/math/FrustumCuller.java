package com.terralite.render.math;

/**
 * Extracts the 6 view-frustum planes from a column-major VP matrix and tests
 * axis-aligned bounding boxes for visibility.
 *
 * <p>Uses the Gribb / Hartmann method: each plane is a linear combination of
 * the matrix rows.  A chunk AABB is considered visible if its "positive vertex"
 * (the corner most aligned with each plane normal) lies on the positive side of
 * all 6 planes.
 */
public final class FrustumCuller {
    // 6 planes × 4 coefficients (a, b, c, d)  where  ax+by+cz+d >= 0 = inside
    private final float[][] planes = new float[6][4];

    /**
     * Recomputes frustum planes from a column-major float[16] view-projection matrix.
     * Call once per frame before any {@link #isVisible} tests.
     */
    public void update(float[] vp) {
        // Extract rows from column-major storage: row_i = (vp[i], vp[4+i], vp[8+i], vp[12+i])
        // Plane extraction (Gribb/Hartmann):
        //   left   = row3 + row0
        //   right  = row3 - row0
        //   bottom = row3 + row1
        //   top    = row3 - row1
        //   near   = row3 + row2
        //   far    = row3 - row2
        for (int c = 0; c < 4; c++) {
            float r0 = vp[c * 4];
            float r1 = vp[c * 4 + 1];
            float r2 = vp[c * 4 + 2];
            float r3 = vp[c * 4 + 3];

            planes[0][c] = r3 + r0; // left
            planes[1][c] = r3 - r0; // right
            planes[2][c] = r3 + r1; // bottom
            planes[3][c] = r3 - r1; // top
            planes[4][c] = r3 + r2; // near
            planes[5][c] = r3 - r2; // far
        }
    }

    /**
     * Returns {@code false} if the AABB is definitely outside the frustum (safe to skip).
     * Returns {@code true} if the AABB is inside or intersects (must be rendered).
     */
    public boolean isVisible(float minX, float minY, float minZ,
                              float maxX, float maxY, float maxZ) {
        for (float[] p : planes) {
            // Positive vertex: corner most aligned with the plane normal
            float px = p[0] >= 0 ? maxX : minX;
            float py = p[1] >= 0 ? maxY : minY;
            float pz = p[2] >= 0 ? maxZ : minZ;
            if (p[0] * px + p[1] * py + p[2] * pz + p[3] < 0) {
                return false; // entirely outside this plane
            }
        }
        return true;
    }
}
