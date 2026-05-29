package com.terralite.tools.render;

import com.terralite.render.ClearColor;
import com.terralite.render.RenderFrame;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;
import com.terralite.render.backend.OpenGlRenderBackend;
import com.terralite.render.glfw.GlfwWindow;
import com.terralite.render.window.WindowConfig;

public final class OpenGlSmoke {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final long DEFAULT_DURATION_MILLIS = 5_000L;

    private OpenGlSmoke() {
    }

    public static void main(String[] args) throws InterruptedException {
        long durationMillis = durationMillis(args);
        GlfwWindow window = new GlfwWindow(WindowConfig.openGl("TERRALITE OpenGL Smoke", WIDTH, HEIGHT));
        Renderer renderer = new Renderer(new OpenGlRenderBackend(window));

        try {
            renderer.start();
            long startedAt = System.nanoTime();
            long deadline = startedAt + durationMillis * 1_000_000L;

            while (!window.shouldClose() && System.nanoTime() < deadline) {
                float t = (System.nanoTime() - startedAt) / 1_000_000_000.0f;
                ClearColor color = new ClearColor(
                        channel(t, 0.0f),
                        channel(t, 2.0f),
                        channel(t, 4.0f),
                        1.0f
                );
                renderer.render(new RenderFrame(new Viewport(WIDTH, HEIGHT), color));
                Thread.sleep(16L);
            }
        } finally {
            renderer.stop();
        }
    }

    private static float channel(float timeSeconds, float phase) {
        return (float) ((Math.sin(timeSeconds + phase) + 1.0) * 0.5);
    }

    private static long durationMillis(String[] args) {
        if (args.length == 0) {
            return DEFAULT_DURATION_MILLIS;
        }

        long seconds = Long.parseLong(args[0]);
        if (seconds <= 0L) {
            throw new IllegalArgumentException("Duration seconds must be positive");
        }
        return seconds * 1_000L;
    }
}
