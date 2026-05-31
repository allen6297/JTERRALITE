package com.terralite.tools.render;

import com.terralite.content.assets.ContentAssetIndex;
import com.terralite.content.assets.ContentModelIndex;
import com.terralite.content.assets.model.ContentModelMeshLibrary;
import com.terralite.engine.camera.Camera;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.world.World;
import com.terralite.game.content.GameContentLoadReport;
import com.terralite.game.content.GameContentLoader;
import com.terralite.render.ClearColor;
import com.terralite.render.Renderer;
import com.terralite.render.backend.VulkanRenderBackend;
import com.terralite.render.glfw.GlfwWindow;
import com.terralite.render.texture.TextureAtlasBuilder;
import com.terralite.render.window.WindowConfig;
import com.terralite.runtime.render.RenderPipeline;
import com.terralite.runtime.world.RuntimeWorldFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Manual smoke test that loads the repo's real content packs and renders engine
 * world chunks through the runtime render pipeline.
 *
 * <p>Run via: {@code .\gradlew.bat :tools:runContentRenderSmoke}
 * <p>Optional args: {@code <packsRoot> <durationSeconds>}.
 */
public final class ContentRenderSmoke {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final long DEFAULT_DURATION_MILLIS = 5_000L;

    private ContentRenderSmoke() {
    }

    public static void main(String[] args) throws Exception {
        SmokeConfig config = SmokeConfig.from(args);
        GameContentLoadReport content = new GameContentLoader().load(config.packsRoot());
        var assets = ContentAssetIndex.load(content.packs());
        var modelMeshes = new ContentModelMeshLibrary().loadSupported(ContentModelIndex.load(content.packs()));
        var textureAtlas = new TextureAtlasBuilder().build(assets.assets().stream()
                .filter(asset -> asset.type().equals("textures"))
                .collect(Collectors.toMap(asset -> asset.id(), asset -> asset.path())));
        World world = new RuntimeWorldFactory().create(content.gameData());
        Camera camera = new Camera(
                new Transform(0.0, 4.0, 10.0),
                70.0,
                0.1,
                1000.0,
                0.0,
                -15.0
        );

        GlfwWindow window = new GlfwWindow(WindowConfig.vulkan(
                "TERRALITE Content Render Smoke - " + content.packs().size() + " pack(s)",
                WIDTH,
                HEIGHT
        ));
        VulkanRenderBackend backend = new VulkanRenderBackend(window);
        Renderer renderer = new Renderer(backend);

        try {
            renderer.start();
            RenderPipeline pipeline = new RenderPipeline(
                    world,
                    camera,
                    renderer,
                    window.viewport(),
                    new ClearColor(0.04f, 0.08f, 0.10f, 1.0f),
                    content.gameData(),
                    modelMeshes,
                    textureAtlas
            );

            long startedAt = System.nanoTime();
            long deadline = startedAt + config.durationMillis() * 1_000_000L;
            while (!backend.shouldClose() && System.nanoTime() < deadline) {
                double t = (System.nanoTime() - startedAt) / 1_000_000_000.0;
                camera.setYaw(t * 30.0);
                pipeline.setViewport(window.viewport());
                pipeline.setClearColor(new ClearColor(channel(t, 0.0), channel(t, 2.0), channel(t, 4.0), 1.0f));
                pipeline.renderFrame();
                Thread.sleep(16L);
            }
        } finally {
            renderer.stop();
        }
    }

    private static float channel(double t, double phase) {
        return (float) ((Math.sin(t + phase) + 1.0) * 0.5) * 0.3f + 0.05f;
    }

    private record SmokeConfig(Path packsRoot, long durationMillis) {
        static SmokeConfig from(String[] args) throws IOException {
            Path packsRoot = args.length > 0
                    ? Path.of(args[0]).toAbsolutePath().normalize()
                    : Path.of("packs").toAbsolutePath().normalize();
            long durationMillis = args.length > 1
                    ? parseDurationMillis(args[1])
                    : DEFAULT_DURATION_MILLIS;
            return new SmokeConfig(packsRoot, durationMillis);
        }

        private static long parseDurationMillis(String secondsArgument) {
            long seconds = Long.parseLong(secondsArgument);
            if (seconds <= 0) {
                throw new IllegalArgumentException("Duration must be positive");
            }
            return seconds * 1_000L;
        }
    }
}
