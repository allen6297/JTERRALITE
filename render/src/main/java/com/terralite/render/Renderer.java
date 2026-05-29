package com.terralite.render;

import java.util.Objects;

public final class Renderer {
    private final RenderBackend backend;
    private RenderState state = RenderState.CREATED;

    public Renderer(RenderBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
        backend.initialize();
    }

    public RenderState state() {
        return state;
    }

    public void start() {
        if (state == RenderState.RUNNING) {
            return;
        }

        if (state == RenderState.STOPPED) {
            throw new IllegalStateException("Renderer cannot be restarted after stop");
        }

        backend.start();
        state = RenderState.RUNNING;
    }

    public RenderStats render(RenderFrame frame) {
        Objects.requireNonNull(frame, "frame");
        ensureRunning();
        return backend.render(frame);
    }

    public void stop() {
        if (state != RenderState.RUNNING) {
            return;
        }

        backend.stop();
        state = RenderState.STOPPED;
    }

    private void ensureRunning() {
        if (state != RenderState.RUNNING) {
            throw new IllegalStateException("Renderer is not running");
        }
    }
}
