package com.terralite.render.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Matrix4fTest {
    private static final float EPSILON = 0.0001f;

    @Test
    void identityMultipliedByItselfIsIdentity() {
        Matrix4f identity = Matrix4f.identity();
        float[] result = identity.multiply(identity).toArray();
        float[] expected = Matrix4f.identity().toArray();

        for (int i = 0; i < 16; i++) {
            assertEquals(expected[i], result[i], EPSILON, "index " + i);
        }
    }

    @Test
    void perspectiveMatrixHasCorrectDiagonal() {
        float fovY = (float) Math.toRadians(70.0);
        float aspect = 16.0f / 9.0f;
        float near = 0.1f;
        float far = 1000.0f;

        float[] m = Matrix4f.perspective(fovY, aspect, near, far).toArray();
        float f = 1.0f / (float) Math.tan(fovY * 0.5f);

        assertEquals(f / aspect, m[0],  EPSILON);  // m[0] = f/aspect
        assertEquals(-f,         m[5],  EPSILON);  // m[5] = -f for Vulkan Y-down NDC
        assertEquals(-1.0f,      m[11], EPSILON);  // m[11] = -1 (RH perspective)
        assertEquals(0.0f,       m[15], EPSILON);  // m[15] = 0
    }

    @Test
    void lookAtAtOriginLookingMinusZTranslatesByEye() {
        // Camera at (3, 5, 10) looking along -Z
        float[] m = Matrix4f.lookAt(3, 5, 10, 3, 5, 9, 0, 1, 0).toArray();

        // Right = (1,0,0), Up = (0,1,0), -Forward = (0,0,1)
        assertEquals(1.0f,  m[0],  EPSILON);   // right.x
        assertEquals(0.0f,  m[1],  EPSILON);   // up.x
        assertEquals(0.0f,  m[2],  EPSILON);   // -fwd.x
        assertEquals(0.0f,  m[4],  EPSILON);   // right.y
        assertEquals(1.0f,  m[5],  EPSILON);   // up.y
        assertEquals(0.0f,  m[6],  EPSILON);   // -fwd.y
        assertEquals(0.0f,  m[8],  EPSILON);   // right.z
        assertEquals(0.0f,  m[9],  EPSILON);   // up.z
        assertEquals(1.0f,  m[10], EPSILON);   // -fwd.z
        // translation column
        assertEquals(-3.0f, m[12], EPSILON);   // -dot(right, eye)
        assertEquals(-5.0f, m[13], EPSILON);   // -dot(up, eye)
        assertEquals(-10.0f, m[14], EPSILON);  // dot(fwd, eye) = dot((0,0,-1),(3,5,10)) = -10
        assertEquals(1.0f,  m[15], EPSILON);
    }

    @Test
    void cameraMatricesProducesFloat16() {
        com.terralite.render.RenderCamera camera = new com.terralite.render.RenderCamera(
                0.0f, 0.0f, 10.0f, 70.0f, 0.1f, 1000.0f);
        com.terralite.render.Viewport viewport = new com.terralite.render.Viewport(1280, 720);

        float[] mvp = CameraMatrices.viewProjection(camera, viewport);

        assertEquals(16, mvp.length);
    }
}
