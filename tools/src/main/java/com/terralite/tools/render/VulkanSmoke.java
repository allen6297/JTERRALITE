package com.terralite.tools.render;

import com.terralite.render.ClearColor;
import com.terralite.render.RenderCamera;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderScene;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;
import com.terralite.render.backend.VulkanRenderBackend;
import com.terralite.render.glfw.GlfwWindow;
import com.terralite.render.window.WindowConfig;

/**
 * Manual smoke test for the Vulkan renderer. Opens a window, submits a small
 * grid of chunk markers, and renders them with a perspective camera.
 *
 * <p>Run via: {@code .\gradlew.bat :tools:runVulkanSmoke}
 * <p>Optional argument: duration in seconds (default 5).
 */
public final class VulkanSmoke {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final long DEFAULT_DURATION_MILLIS = 5_000L;

    private VulkanSmoke() {
    }

    public static void main(String[] args) throws InterruptedException {
        long durationMillis = durationMillis(args);
        GlfwWindow window = new GlfwWindow(WindowConfig.vulkan("TERRALITE Vulkan Smoke", WIDTH, HEIGHT));
        Renderer renderer = new Renderer(new VulkanRenderBackend(window));

        try {
            renderer.start();
            long startedAt = System.nanoTime();
            long deadline = startedAt + durationMillis * 1_000_000L;

            // Camera positioned above origin, looking down toward chunks
            RenderCamera camera = new RenderCamera(0.0, 4.0, 12.0, 70.0, 0.1, 1000.0);

            // 3x3 grid of chunks at z=0
            RenderScene.Builder sceneBuilder = RenderScene.builder().camera(camera);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    sceneBuilder.addChunk(new RenderChunk(x, 0, z));
                }
            }
            RenderScene scene = sceneBuilder.build();

            while (!window.shouldClose() && System.nanoTime() < deadline) {
                float t = (System.nanoTime() - startedAt) / 1_000_000_000.0f;
                ClearColor clearColor = new ClearColor(
                        channel(t, 0.0f),
                        channel(t, 2.0f),
                        channel(t, 4.0f),
                        1.0f
                );
                renderer.render(new RenderFrame(new Viewport(WIDTH, HEIGHT), clearColor, scene));
                Thread.sleep(16L);
            }
        } finally {
            renderer.stop();
        }
    }

    private static float channel(float t, float phase) {
        return (float) ((Math.sin(t + phase) + 1.0) * 0.5) * 0.3f + 0.05f;
    }

    private static long durationMillis(String[] args) {
        if (args.length == 0) return DEFAULT_DURATION_MILLIS;
        long seconds = Long.parseLong(args[0]);
        if (seconds <= 0) throw new IllegalArgumentException("Duration must be positive");
        return seconds * 1_000L;
    }
}
