package com.terralite.launcher;

import com.terralite.core.logging.Loggers;
import com.terralite.engine.camera.Camera;
import com.terralite.engine.world.World;
import com.terralite.game.content.GameContentLoadReport;
import com.terralite.game.content.GameContentLoader;
import com.terralite.render.ClearColor;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;
import com.terralite.render.backend.VulkanRenderBackend;
import com.terralite.render.glfw.GlfwWindow;
import com.terralite.render.window.WindowConfig;
import com.terralite.runtime.render.RenderPipeline;
import com.terralite.runtime.world.RuntimeWorldFactory;
import com.terralite.server.TerraliteServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

final class ClientApp {
    private static final Logger log = Loggers.get(ClientApp.class);

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final ClearColor SKY_COLOR = new ClearColor(0.53f, 0.81f, 0.98f, 1.0f);

    private final Path packsRoot;

    ClientApp(Path packsRoot) {
        this.packsRoot = Objects.requireNonNull(packsRoot, "packsRoot");
    }

    void run() throws Exception {
        GameContentLoadReport content = new GameContentLoader().load(packsRoot);
        log.info("Loaded {} content pack(s)", content.packs().size());

        World world = new RuntimeWorldFactory().create(content.gameData());

        TerraliteServer server = TerraliteServer.builder()
                .world(world)
                .contentPacks(content.packs())
                .build();

        GlfwWindow window = new GlfwWindow(WindowConfig.vulkan("TERRALITE", WIDTH, HEIGHT));
        VulkanRenderBackend backend = new VulkanRenderBackend(window);

        try (Renderer renderer = new Renderer(backend)) {
            renderer.start();

            Camera camera = new Camera();
            Viewport initialViewport = window.viewport();
            RenderPipeline pipeline = new RenderPipeline(
                    world, camera, renderer, initialViewport, SKY_COLOR, content.gameData()
            );

            server.start();
            log.info("Client running. Close the window to exit.");

            long last = System.nanoTime();
            while (!backend.shouldClose()) {
                long now = System.nanoTime();
                server.advance(Duration.ofNanos(now - last));
                last = now;

                pipeline.setViewport(window.viewport());
                pipeline.renderFrame();
            }

            server.stop();
        }
    }
}
