package com.terralite.launcher;

import com.terralite.content.assets.ContentAssetIndex;
import com.terralite.content.assets.ContentModelIndex;
import com.terralite.content.assets.model.ContentModelMeshLibrary;
import com.terralite.engine.camera.Camera;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.entity.EntitySpawnRequest;
import com.terralite.engine.game.GameEngine;
import com.terralite.engine.input.InputActions;
import com.terralite.engine.input.InputState;
import com.terralite.engine.physics.BlockCollisionResolutionSystem;
import com.terralite.engine.physics.Collider;
import com.terralite.engine.physics.GravitySystem;
import com.terralite.engine.physics.MovementSystem;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.player.PlayerControlled;
import com.terralite.engine.player.YawRelativePlayerInputSystem;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.terrain.ChunkLoadRadius;
import com.terralite.engine.terrain.ChunkLoaderSystem;
import com.terralite.engine.terrain.ChunkUnloaderSystem;
import com.terralite.engine.world.World;
import com.terralite.game.content.GameContentLoadReport;
import com.terralite.game.content.GameContentLoader;
import com.terralite.launcher.camera.CameraMode;
import com.terralite.launcher.camera.PlayerCameraSystem;
import com.terralite.launcher.input.MouseLookController;
import com.terralite.launcher.interaction.BlockInteractionSystem;
import com.terralite.launcher.save.GameSaveSystem;
import com.terralite.runtime.terrain.TerrainGenerator;
import com.terralite.runtime.terrain.TerrainGeneratorSystem;
import com.terralite.render.ClearColor;
import com.terralite.render.Renderer;
import com.terralite.render.backend.VulkanRenderBackend;
import com.terralite.render.glfw.GlfwWindow;
import com.terralite.render.texture.TextureAtlasBuilder;
import com.terralite.render.window.WindowConfig;
import com.terralite.runtime.render.RenderPipeline;
import com.terralite.runtime.world.RuntimeWorldFactory;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Main entry point for the Terralite game client.
 *
 * <p>Usage: {@code ./gradlew :launcher:runGame [packsRoot]}
 *
 * <p>Controls:
 * <ul>
 *   <li>WASD — move</li>
 *   <li>Mouse — look</li>
 *   <li>Space — jump</li>
 *   <li>Left Shift — sprint</li>
 *   <li>Left-click (hold) — break block</li>
 *   <li>Right-click — place block</li>
 *   <li>F5 — toggle first/third-person camera</li>
 *   <li>Escape — release cursor; press again to exit</li>
 * </ul>
 */
public final class TerraliteGame {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final double PLAYER_SPEED = 5.0;
    private static final long TARGET_FRAME_NANOS = 1_000_000_000L / 60; // 60 fps cap
    private static final ClearColor SKY_COLOR = new ClearColor(0.45f, 0.65f, 0.85f, 1.0f);

    private TerraliteGame() {
    }

    public static void main(String[] args) throws Exception {
        Path packsRoot = args.length > 0
                ? Path.of(args[0]).toAbsolutePath().normalize()
                : Path.of("packs").toAbsolutePath().normalize();

        // --- Content loading ---
        GameContentLoadReport content = new GameContentLoader().load(packsRoot);
        var assets     = ContentAssetIndex.load(content.packs());
        var modelMeshes = new ContentModelMeshLibrary()
                .loadSupported(ContentModelIndex.load(content.packs()));
        var textureAtlas = new TextureAtlasBuilder().build(
                assets.assets().stream()
                        .filter(a -> a.type().equals("textures"))
                        .collect(Collectors.toMap(a -> a.id(), a -> a.path())));

        // --- World & player ---
        RenderPipeline[] pipelineRef = {null};

        long seed = System.currentTimeMillis();
        var terrainGenerator = new TerrainGenerator(seed);
        var terrainSystem    = new TerrainGeneratorSystem(terrainGenerator)
                .onChunkReady(chunkPos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirty(chunkPos); });

        World world = new RuntimeWorldFactory().createEmpty(content.gameData());
        Camera camera = new Camera(new Transform(0.0, 5.0, 0.0), 70.0, 0.1, 1000.0);
        InputState input = new InputState();

        // Spawn above the expected terrain surface; gravity will drop the player to ground
        int spawnY = terrainGenerator.surfaceHeight(8, 8) + 5;
        Entity player = world.spawner().spawn(EntitySpawnRequest.create()
                .transform(new Transform(8.0, spawnY, 8.0))
                .playerControlled(new PlayerControlled(PLAYER_SPEED)));
        player.set(PhysicsComponents.COLLIDER, new Collider(0.3, 0.9, 0.3));

        var cameraSystem = new PlayerCameraSystem(camera, player.id(), CameraMode.FIRST_PERSON);

        var blockInteraction = new BlockInteractionSystem(
                input, camera, player.id(), RuntimeWorldFactory.DEFAULT_SURFACE_BLOCK,
                chunkPos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirty(chunkPos); });

        // --- Engine ---
        GameEngine engine = GameEngine.builder()
                .world(world)
                .input(input)
                .addWorldSimulationSystem(new YawRelativePlayerInputSystem(input, camera))
                .addWorldSimulationSystem(new GravitySystem())
                .addWorldSimulationSystem(new MovementSystem())
                .addWorldSimulationSystem(new BlockCollisionResolutionSystem())
                .addWorldSimulationSystem(blockInteraction)
                .addWorldSimulationSystem(cameraSystem)
                .addWorldSimulationSystem(new ChunkLoaderSystem(
                        player.id(), RuntimeWorldFactory.CHUNK_SIZE, new ChunkLoadRadius(4, 1)))
                .addWorldSimulationSystem(new ChunkUnloaderSystem(
                        player.id(), RuntimeWorldFactory.CHUNK_SIZE, new ChunkLoadRadius(6, 2)))
                .addWorldSimulationSystem(terrainSystem)
                .tickDelta(Duration.ofMillis(50))
                .build();
        engine.start();

        // Pre-generate spawn area synchronously so the player lands on solid ground
        int spawnCX = Math.floorDiv(8, RuntimeWorldFactory.CHUNK_SIZE);
        int spawnCZ = Math.floorDiv(8, RuntimeWorldFactory.CHUNK_SIZE);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    ChunkPos pos = ChunkPos.of(spawnCX + dx, dy, spawnCZ + dz);
                    if (!world.containsChunk(pos)) world.putChunk(new Chunk(pos));
                    terrainSystem.generateNow(world, pos);
                }
            }
        }

        // --- Window & renderer ---
        GlfwWindow window = new GlfwWindow(WindowConfig.vulkan("TERRALITE", WIDTH, HEIGHT));
        VulkanRenderBackend backend = new VulkanRenderBackend(window);
        Renderer renderer = new Renderer(backend);

        try {
            MouseLookController mouseLook = new MouseLookController(camera);
            mouseLook.install(window.handle());

            boolean[] f5WasDown = {false};

            GLFW.glfwSetMouseButtonCallback(window.handle(), (win, button, action, mods) -> {
                if (!mouseLook.isCaptured()) {
                    if (action == GLFW.GLFW_PRESS) mouseLook.capture(win);
                    return;
                }
                boolean pressed = action != GLFW.GLFW_RELEASE;
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)  input.setPressed(InputActions.BREAK_BLOCK, pressed);
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) input.setPressed(InputActions.PLACE_BLOCK, pressed);
            });

            renderer.start();
            mouseLook.capture(window.handle());

            pipelineRef[0] = new RenderPipeline(
                    world, camera, renderer, window.viewport(), SKY_COLOR,
                    content.gameData(), modelMeshes, textureAtlas);

            GameSaveSystem saveSystem = new GameSaveSystem(world, player.id(), camera, pipelineRef[0])
                    .onLoad(terrainSystem::reset);

            GLFW.glfwSetKeyCallback(window.handle(), (win, key, scancode, action, mods) -> {
                boolean pressed = action != GLFW.GLFW_RELEASE;
                boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;

                // Ctrl+S — save,  Ctrl+L — load
                if (ctrl && key == GLFW.GLFW_KEY_S && action == GLFW.GLFW_PRESS) {
                    saveSystem.save();
                    return;
                }
                if (ctrl && key == GLFW.GLFW_KEY_L && action == GLFW.GLFW_PRESS) {
                    saveSystem.load();
                    return;
                }

                switch (key) {
                    case GLFW.GLFW_KEY_W          -> input.setPressed(InputActions.MOVE_FORWARD, pressed);
                    case GLFW.GLFW_KEY_S          -> { if (!ctrl) input.setPressed(InputActions.MOVE_BACK, pressed); }
                    case GLFW.GLFW_KEY_A          -> input.setPressed(InputActions.MOVE_LEFT,    pressed);
                    case GLFW.GLFW_KEY_D          -> input.setPressed(InputActions.MOVE_RIGHT,   pressed);
                    case GLFW.GLFW_KEY_SPACE      -> input.setPressed(InputActions.JUMP,         pressed);
                    case GLFW.GLFW_KEY_LEFT_SHIFT -> input.setPressed(InputActions.SPRINT,       pressed);
                    case GLFW.GLFW_KEY_F5 -> {
                        if (action == GLFW.GLFW_PRESS && !f5WasDown[0]) {
                            f5WasDown[0] = true;
                            cameraSystem.setMode(cameraSystem.mode() == CameraMode.FIRST_PERSON
                                    ? CameraMode.THIRD_PERSON : CameraMode.FIRST_PERSON);
                        } else if (action == GLFW.GLFW_RELEASE) {
                            f5WasDown[0] = false;
                        }
                    }
                    case GLFW.GLFW_KEY_ESCAPE -> {
                        if (action == GLFW.GLFW_PRESS) {
                            if (mouseLook.isCaptured()) mouseLook.release(win);
                            else GLFW.glfwSetWindowShouldClose(win, true);
                        }
                    }
                }
            });

            long lastNanos = System.nanoTime();
            while (!backend.shouldClose()) {
                long frameStart = System.nanoTime();
                window.pollEvents();

                long now = System.nanoTime();
                engine.advance(Duration.ofNanos(now - lastNanos));
                lastNanos = now;

                pipelineRef[0].setViewport(window.viewport());
                pipelineRef[0].setSelection(blockInteraction.currentTarget().orElse(null));
                pipelineRef[0].renderFrame();

                // Sleep for the remainder of the target frame time to cap at 60 fps
                long elapsed = System.nanoTime() - frameStart;
                long sleepNanos = TARGET_FRAME_NANOS - elapsed;
                if (sleepNanos > 1_000_000L) {
                    Thread.sleep(sleepNanos / 1_000_000L);
                }
            }
        } finally {
            terrainSystem.shutdown();
            engine.stop();
            renderer.stop();
        }
    }
}
