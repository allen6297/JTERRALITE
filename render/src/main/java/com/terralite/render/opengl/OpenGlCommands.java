package com.terralite.render.opengl;

import com.terralite.render.ClearColor;
import com.terralite.render.Viewport;
import com.terralite.render.mesh.DebugMesh;

public interface OpenGlCommands {
    void createCapabilities();

    int createMesh(DebugMesh mesh);

    void viewport(Viewport viewport);

    void clear(ClearColor clearColor);

    void drawMesh(int meshHandle);

    void destroyMesh(int meshHandle);
}
