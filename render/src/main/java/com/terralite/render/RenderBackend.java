package com.terralite.render;

public interface RenderBackend {
    default void initialize() {
    }

    default void start() {
    }

    RenderStats render(RenderFrame frame);

    default void stop() {
    }
}
