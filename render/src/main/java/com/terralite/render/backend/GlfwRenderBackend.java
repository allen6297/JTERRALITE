package com.terralite.render.backend;

import com.terralite.render.RenderBackend;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderStats;
import com.terralite.render.Viewport;
import com.terralite.render.window.RenderWindow;

import java.util.Objects;

public final class GlfwRenderBackend implements RenderBackend {
    private final RenderWindow window;
    private long frameIndex;

    public GlfwRenderBackend(RenderWindow window) {
        this.window = Objects.requireNonNull(window, "window");
    }

    public RenderWindow window() {
        return window;
    }

    @Override
    public void initialize() {
        window.create();
    }

    @Override
    public void start() {
        window.show();
    }

    @Override
    public RenderStats render(RenderFrame frame) {
        Objects.requireNonNull(frame, "frame");
        window.pollEvents();
        Viewport viewport = window.viewport();
        return new RenderStats(++frameIndex, viewport);
    }

    @Override
    public void stop() {
        window.destroy();
    }
}
