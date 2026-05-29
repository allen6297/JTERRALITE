package com.terralite.render.window;

import com.terralite.render.Viewport;

public interface RenderWindow {
    WindowConfig config();

    WindowState state();

    void create();

    void show();

    default void makeContextCurrent() {
        throw new UnsupportedOperationException("Window does not expose a graphics context");
    }

    default void swapBuffers() {
        throw new UnsupportedOperationException("Window does not expose swap buffers");
    }

    void pollEvents();

    boolean shouldClose();

    Viewport viewport();

    void destroy();
}
