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
 * Manual smoke test for the Vulkan renderer. Opens a window, submits a 3×3 grid of
 * chunk markers, and slowly rotates the camera yaw to exercise the MVP transform.
 * The window can be resized — the swapchain recreates automatically.
 *
 * <p>Run via: {@code .\gradlew.bat :tools:runVulkanSmoke}
 * <p>Optional argument: duration in seconds (default 5).
 */
public final class VulkanSmoke {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final long DEFAULT_DURATION_MILLIS = 5_000L;

    /** Camera position — above and behind the chunk grid. */
    private static final double EYE_X = 0.0;
    private static final double EYE_Y = 4.0;
    private static final double EYE_Z = 10.0;

    /** Pitch angle so the camera looks slightly downward toward the grid. */
    private static final double PITCH_DEGREES = -15.0;

    /** Yaw rotation speed in degrees per second. */
    private static final double YAW_DEGREES_PER_SECOND = 30.0;

    private VulkanSmoke() {
    }

    public static void main(String[] args) throws InterruptedException {
        long durationMillis = durationMillis(args);
        GlfwWindow window = new GlfwWindow(WindowConfig.vulkan("TERRALITE Vulkan Smoke", WIDTH, HEIGHT));
        VulkanRenderBackend backend = new VulkanRenderBackend(window);
        Renderer renderer = new Renderer(backend);

        try {
            renderer.start();
            long startedAt = System.nanoTime();
            long deadline = startedAt + durationMillis * 1_000_000L;

            // 3×3 chunk grid at y=0
            RenderScene.Builder sceneBuilder = RenderScene.builder();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    sceneBuilder.addChunk(new RenderChunk(x, 0, z));
                }
            }
            RenderScene baseScene = sceneBuilder.build();

            while (!backend.shouldClose() && System.nanoTime() < deadline) {
                double t = (System.nanoTime() - startedAt) / 1_000_000_000.0;

                // Slowly rotate yaw so rotation is visible
                double yaw = t * YAW_DEGREES_PER_SECOND;

                // Gentle sky color pulse
                ClearColor clearColor = new ClearColor(
                        channel(t, 0.0), channel(t, 2.0), channel(t, 4.0), 1.0f
                );

                RenderCamera camera = new RenderCamera(
                        EYE_X, EYE_Y, EYE_Z,
                        70.0, 0.1, 1000.0,
                        yaw, PITCH_DEGREES
                );

                RenderScene scene = RenderScene.builder()
                        .camera(camera)
                        .addChunks(baseScene.chunks())
                        .build();

                // Use the actual window size so resize is handled correctly
                Viewport viewport = window.viewport();
                renderer.render(new RenderFrame(viewport, clearColor, scene));
                Thread.sleep(16L);
            }
        } finally {
            renderer.stop();
        }
    }

    private static float channel(double t, double phase) {
        return (float) ((Math.sin(t + phase) + 1.0) * 0.5) * 0.3f + 0.05f;
    }

    private static long durationMillis(String[] args) {
        if (args.length == 0) return DEFAULT_DURATION_MILLIS;
        long seconds = Long.parseLong(args[0]);
        if (seconds <= 0) throw new IllegalArgumentException("Duration must be positive");
        return seconds * 1_000L;
    }
}
