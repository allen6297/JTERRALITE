package com.terralite.render.opengl;

import com.terralite.render.ClearColor;
import com.terralite.render.Viewport;
import com.terralite.render.mesh.DebugMesh;

public interface OpenGlCommands {
    void createCapabilities();

    int createMesh(DebugMesh mesh);

    void viewport(Viewport viewport);

    void clear(ClearColor clearColor);

    /**
     * Draw the mesh identified by {@code meshHandle} using the supplied column-major
     * 4x4 MVP matrix (float[16]).
     */
    void drawMesh(int meshHandle, float[] mvp);

    void destroyMesh(int meshHandle);
}
