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
import com.terralite.engine.terrain.BlockRaycaster;
import com.terralite.engine.world.World;
import com.terralite.game.content.GameContentLoadReport;
import com.terralite.game.content.GameContentLoader;
import com.terralite.launcher.camera.CameraMode;
import com.terralite.launcher.camera.PlayerCameraSystem;
import com.terralite.launcher.input.MouseLookController;
import com.terralite.launcher.save.GameSaveSystem;
import com.terralite.runtime.interaction.BlockInspectionInfo;
import com.terralite.runtime.interaction.BlockInteractionSystem;
import com.terralite.runtime.terrain.TerrainGenerator;
import com.terralite.runtime.terrain.TerrainGeneratorSystem;
import com.terralite.render.ClearColor;
import com.terralite.render.Renderer;
import com.terralite.render.backend.VulkanRenderBackend;
import com.terralite.render.glfw.GlfwWindow;
import com.terralite.render.texture.TextureAtlasBuilder;
import com.terralite.render.window.AwtRenderWindow;
import com.terralite.render.window.WindowConfig;
import com.terralite.runtime.render.RenderPipeline;
import com.terralite.runtime.world.RuntimeWorldFactory;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.awt.Canvas;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    private enum GameState { PLAYING, PAUSED, CHAT }

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
        com.terralite.runtime.render.PlayerAssetInstaller.bootstrap(packsRoot);
        GameContentLoadReport content = new GameContentLoader().load(packsRoot);
        var assets     = ContentAssetIndex.load(content.packs());
        var modelMeshes = new ContentModelMeshLibrary()
                .loadSupported(ContentModelIndex.load(content.packs()));
        var playerModel = modelMeshes.get(
                com.terralite.runtime.render.PlayerBoxBuilder.MODEL_ID);
        var textureAtlas = new TextureAtlasBuilder().build(
                assets.assets().stream()
                        .filter(a -> a.type().equals("textures"))
                        .collect(Collectors.toMap(a -> a.id(), a -> a.path())));

        // --- World & player ---
        RenderPipeline[] pipelineRef = {null};

        long seed = System.currentTimeMillis();
        var terrainGenerator = new TerrainGenerator(seed);
        var terrainSystem    = new TerrainGeneratorSystem(terrainGenerator)
                .onChunkReady(chunkPos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirtyWithNeighbors(chunkPos); });

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
                chunkPos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirtyWithNeighbors(chunkPos); });

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
                .tickDeltaMillis(50)
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
            boolean[] inspecting = {false};
            String[] lastWindowTitle = {"TERRALITE"};
            BlockRaycaster.HitResult[] lastInspectionTarget = {null};

            // --- UI state ---
            GameState[] uiState  = {GameState.PLAYING};
            com.terralite.runtime.render.PauseMenuOverlay.Item[] menuSel = {
                com.terralite.runtime.render.PauseMenuOverlay.Item.RESUME
            };
            List<String>   chatMessages = new ArrayList<>();
            StringBuilder  chatInput    = new StringBuilder();

            GLFW.glfwSetMouseButtonCallback(window.handle(), (win, button, action, mods) -> {
                if (inspecting[0]) {
                    return;
                }
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
                boolean ctrl    = (mods & GLFW.GLFW_MOD_CONTROL) != 0;

                // Ctrl+S — save,  Ctrl+L — load (always available)
                if (ctrl && key == GLFW.GLFW_KEY_S && action == GLFW.GLFW_PRESS) { saveSystem.save(); return; }
                if (ctrl && key == GLFW.GLFW_KEY_L && action == GLFW.GLFW_PRESS) { saveSystem.load(); return; }

                switch (key) {
                    case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> {
                        // Polled each frame — handled outside the callback.
                    }

                    // --- Movement (only when playing) ---
                    case GLFW.GLFW_KEY_W -> {
                        if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.MOVE_FORWARD, pressed);
                    }
                    case GLFW.GLFW_KEY_S -> {
                        if (!ctrl && uiState[0] == GameState.PLAYING) input.setPressed(InputActions.MOVE_BACK, pressed);
                    }
                    case GLFW.GLFW_KEY_A -> {
                        if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.MOVE_LEFT, pressed);
                    }
                    case GLFW.GLFW_KEY_D -> {
                        if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.MOVE_RIGHT, pressed);
                    }
                    case GLFW.GLFW_KEY_SPACE -> {
                        if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.JUMP, pressed);
                    }
                    case GLFW.GLFW_KEY_LEFT_SHIFT -> {
                        if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.SPRINT, pressed);
                    }

                    // --- Camera toggle (only when playing) ---
                    case GLFW.GLFW_KEY_F5 -> {
                        if (action == GLFW.GLFW_PRESS && !f5WasDown[0] && uiState[0] == GameState.PLAYING) {
                            f5WasDown[0] = true;
                            cameraSystem.setMode(cameraSystem.mode() == CameraMode.FIRST_PERSON
                                    ? CameraMode.THIRD_PERSON : CameraMode.FIRST_PERSON);
                        } else if (action == GLFW.GLFW_RELEASE) {
                            f5WasDown[0] = false;
                        }
                    }

                    // --- Open chat (only when playing) ---
                    case GLFW.GLFW_KEY_T -> {
                        if (action == GLFW.GLFW_PRESS && uiState[0] == GameState.PLAYING) {
                            uiState[0] = GameState.CHAT;
                            input.setPressed(InputActions.MOVE_FORWARD, false);
                            input.setPressed(InputActions.MOVE_BACK,    false);
                            input.setPressed(InputActions.MOVE_LEFT,    false);
                            input.setPressed(InputActions.MOVE_RIGHT,   false);
                            mouseLook.release(win);
                        }
                    }

                    // --- Escape — context-sensitive ---
                    case GLFW.GLFW_KEY_ESCAPE -> {
                        if (action == GLFW.GLFW_PRESS) {
                            switch (uiState[0]) {
                                case PLAYING -> {
                                    uiState[0]  = GameState.PAUSED;
                                    menuSel[0]  = com.terralite.runtime.render.PauseMenuOverlay.Item.RESUME;
                                    input.setPressed(InputActions.MOVE_FORWARD, false);
                                    input.setPressed(InputActions.MOVE_BACK,    false);
                                    input.setPressed(InputActions.MOVE_LEFT,    false);
                                    input.setPressed(InputActions.MOVE_RIGHT,   false);
                                    input.setPressed(InputActions.JUMP,         false);
                                    mouseLook.release(win);
                                }
                                case PAUSED -> {
                                    uiState[0] = GameState.PLAYING;
                                    mouseLook.capture(win);
                                }
                                case CHAT -> {
                                    chatInput.setLength(0);
                                    uiState[0] = GameState.PLAYING;
                                    mouseLook.capture(win);
                                }
                            }
                        }
                    }

                    // --- Pause menu navigation ---
                    case GLFW.GLFW_KEY_UP -> {
                        if (action != GLFW.GLFW_RELEASE && uiState[0] == GameState.PAUSED) {
                            var items = com.terralite.runtime.render.PauseMenuOverlay.Item.values();
                            menuSel[0] = items[(menuSel[0].ordinal() - 1 + items.length) % items.length];
                        }
                    }
                    case GLFW.GLFW_KEY_DOWN -> {
                        if (action != GLFW.GLFW_RELEASE && uiState[0] == GameState.PAUSED) {
                            var items = com.terralite.runtime.render.PauseMenuOverlay.Item.values();
                            menuSel[0] = items[(menuSel[0].ordinal() + 1) % items.length];
                        }
                    }

                    // --- Enter — confirm menu selection or send chat ---
                    case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                        if (action == GLFW.GLFW_PRESS) {
                            if (uiState[0] == GameState.PAUSED) {
                                switch (menuSel[0]) {
                                    case RESUME -> { uiState[0] = GameState.PLAYING; mouseLook.capture(win); }
                                    case QUIT   -> GLFW.glfwSetWindowShouldClose(win, true);
                                }
                            } else if (uiState[0] == GameState.CHAT) {
                                String msg = chatInput.toString().trim();
                                if (!msg.isEmpty()) {
                                    chatMessages.add(msg);
                                    System.out.println("[Chat] " + msg);
                                }
                                chatInput.setLength(0);
                            }
                        }
                    }

                    // --- Backspace for chat input ---
                    case GLFW.GLFW_KEY_BACKSPACE -> {
                        if ((action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)
                                && uiState[0] == GameState.CHAT
                                && chatInput.length() > 0) {
                            chatInput.deleteCharAt(chatInput.length() - 1);
                        }
                    }
                }
            });

            // Character callback: feeds typed text into the chat input buffer
            GLFW.glfwSetCharCallback(window.handle(), (win, codepoint) -> {
                if (uiState[0] == GameState.CHAT && chatInput.length() < 40) {
                    chatInput.appendCodePoint(codepoint);
                }
            });

            long lastNanos = System.nanoTime();
            double walkTime  = 0.0; // advances only while moving
            double totalTime = 0.0; // always advances (drives idle animation)
            while (!backend.shouldClose()) {
                long frameStart = System.nanoTime();
                window.pollEvents();

                boolean uiOpen = uiState[0] != GameState.PLAYING;

                boolean shouldInspect = !uiOpen && isAltDown(window.handle());
                if (shouldInspect != inspecting[0]) {
                    inspecting[0] = shouldInspect;
                    input.setPressed(InputActions.BREAK_BLOCK, false);
                    input.setPressed(InputActions.PLACE_BLOCK, false);
                    if (shouldInspect) {
                        mouseLook.release(window.handle());
                    } else {
                        blockInteraction.setInspectionRay(null);
                        pipelineRef[0].setInspectionTooltip(List.of());
                        lastInspectionTarget[0] = null;
                        setWindowTitle(window.handle(), lastWindowTitle, "TERRALITE");
                        if (!uiOpen) mouseLook.capture(window.handle());
                    }
                }

                if (inspecting[0]) {
                    blockInteraction.setInspectionRay(cursorRay(window.handle(), camera));
                    blockInteraction.refreshTarget(world);
                } else {
                    blockInteraction.setInspectionRay(null);
                }

                long now = System.nanoTime();
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                totalTime += dt;
                boolean moving = input.isPressed(com.terralite.engine.input.InputActions.MOVE_FORWARD)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_BACK)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_LEFT)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_RIGHT);
                if (moving) walkTime += dt;
                // Pause menu freezes the simulation; chat keeps it running
                if (uiState[0] != GameState.PAUSED) {
                    engine.advance((long)(dt * 1_000_000_000.0));
                }

                if (inspecting[0]) {
                    BlockRaycaster.HitResult target = blockInteraction.currentTarget();
                    if (!java.util.Objects.equals(lastInspectionTarget[0], target)) {
                        lastInspectionTarget[0] = target;
                        if (target == null) {
                            pipelineRef[0].setInspectionTooltip(List.of("No block"));
                            setWindowTitle(window.handle(), lastWindowTitle, "TERRALITE - Inspect: no block");
                        } else {
                            pipelineRef[0].setInspectionTooltip(
                                    BlockInspectionInfo.lines(world, content.gameData(), target.blockPos()));
                            setWindowTitle(window.handle(), lastWindowTitle,
                                    BlockInspectionInfo.title(world, content.gameData(), target.blockPos()));
                        }
                    }
                }

                // Show self model in third-person
                if (cameraSystem.mode() == com.terralite.launcher.camera.CameraMode.THIRD_PERSON) {
                    var pt = player.require(com.terralite.engine.physics.PhysicsComponents.TRANSFORM);
                    com.terralite.engine.physics.Collider _collider = player.get(com.terralite.engine.physics.PhysicsComponents.COLLIDER);
                    double halfH = _collider != null ? _collider.halfHeight() : 0.9;
                    com.terralite.runtime.render.AnimationPose selfPose = moving
                            ? com.terralite.runtime.render.PlayerAnimations.walk(walkTime)
                            : com.terralite.runtime.render.PlayerAnimations.idle(totalTime);
                    pipelineRef[0].setPlayerMeshes(java.util.List.of(
                            com.terralite.runtime.render.PlayerBoxBuilder.build(
                                    "_self", pt.x(), pt.y() - halfH, pt.z(),
                                    camera.yaw(), camera.pitch(), playerModel, selfPose)));
                } else {
                    pipelineRef[0].setPlayerMeshes(java.util.List.of());
                }

                pipelineRef[0].setViewport(window.viewport());
                pipelineRef[0].setSelection(blockInteraction.currentTarget());

                // --- Build UI overlays for this frame ---
                List<com.terralite.render.RenderChunkMesh> uiMeshes = new ArrayList<>();
                if (uiState[0] == GameState.PAUSED) {
                    uiMeshes.add(com.terralite.runtime.render.PauseMenuOverlay.build(
                            camera, pipelineRef[0].viewport(), menuSel[0]));
                }
                boolean chatVisible = uiState[0] == GameState.CHAT || !chatMessages.isEmpty();
                if (chatVisible) {
                    uiMeshes.add(com.terralite.runtime.render.ChatOverlay.build(
                            camera, pipelineRef[0].viewport(),
                            chatMessages, chatInput.toString(),
                            uiState[0] == GameState.CHAT));
                }
                pipelineRef[0].setUiOverlays(uiMeshes);

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
            if (pipelineRef[0] != null) pipelineRef[0].shutdown();
            renderer.stop();
        }
    }

    // ─── AWT / Compose path ──────────────────────────────────────────────────

    /**
     * Runs the game loop inside an AWT {@link Canvas} (used when the game is hosted in a
     * Compose Desktop window via {@code SwingPanel}).
     *
     * <p>Blocks until {@code shutdownSignal} is set or the player selects Quit.
     * All input is handled via AWT {@link java.awt.event.KeyListener} / {@link java.awt.event.MouseListener}.
     *
     * @param canvas         the canvas to render into (will be focused immediately)
     * @param packsRoot      path to the content pack directory
     * @param shutdownSignal set to {@code true} externally to stop the loop (e.g. window closed)
     */
    public static void runOnCanvas(Canvas canvas, Path packsRoot, AtomicBoolean shutdownSignal)
            throws Exception {

        // --- Content loading (same as main()) ---
        com.terralite.runtime.render.PlayerAssetInstaller.bootstrap(packsRoot);
        GameContentLoadReport content = new GameContentLoader().load(packsRoot);
        var assets      = ContentAssetIndex.load(content.packs());
        var modelMeshes = new ContentModelMeshLibrary()
                .loadSupported(ContentModelIndex.load(content.packs()));
        var playerModel = modelMeshes.get(
                com.terralite.runtime.render.PlayerBoxBuilder.MODEL_ID);
        var textureAtlas = new TextureAtlasBuilder().build(
                assets.assets().stream()
                        .filter(a -> a.type().equals("textures"))
                        .collect(Collectors.toMap(a -> a.id(), a -> a.path())));

        // --- World & player ---
        RenderPipeline[] pipelineRef = {null};

        long seed = System.currentTimeMillis();
        var terrainGenerator = new TerrainGenerator(seed);
        var terrainSystem    = new TerrainGeneratorSystem(terrainGenerator)
                .onChunkReady(chunkPos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirtyWithNeighbors(chunkPos); });

        World world = new RuntimeWorldFactory().createEmpty(content.gameData());
        Camera camera = new Camera(new Transform(0.0, 5.0, 0.0), 70.0, 0.1, 1000.0);
        InputState input = new InputState();

        int spawnY = terrainGenerator.surfaceHeight(8, 8) + 5;
        Entity player = world.spawner().spawn(EntitySpawnRequest.create()
                .transform(new Transform(8.0, spawnY, 8.0))
                .playerControlled(new PlayerControlled(PLAYER_SPEED)));
        player.set(PhysicsComponents.COLLIDER, new Collider(0.3, 0.9, 0.3));

        var cameraSystem = new PlayerCameraSystem(camera, player.id(), CameraMode.FIRST_PERSON);

        var blockInteraction = new BlockInteractionSystem(
                input, camera, player.id(), RuntimeWorldFactory.DEFAULT_SURFACE_BLOCK,
                chunkPos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirtyWithNeighbors(chunkPos); });

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
                .tickDeltaMillis(50)
                .build();
        engine.start();

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
        AwtRenderWindow window = new AwtRenderWindow(
                canvas,
                WindowConfig.vulkan("TERRALITE", WIDTH, HEIGHT),
                shutdownSignal
        );
        VulkanRenderBackend backend = new VulkanRenderBackend(window);
        Renderer renderer = new Renderer(backend);

        // --- AWT input state ---
        GameState[] uiState   = {GameState.PLAYING};
        com.terralite.runtime.render.PauseMenuOverlay.Item[] menuSel = {
            com.terralite.runtime.render.PauseMenuOverlay.Item.RESUME
        };
        List<String>  chatMessages  = new ArrayList<>();
        StringBuilder chatInput     = new StringBuilder();

        // Mouse-look state (AWT cursor capture via Robot)
        boolean[]   lookCaptured  = {false};
        // Accumulated mouse deltas — the listener ADDS to these; the render loop consumes
        // and resets them.  Protected by mouseDelta's own monitor so no delta is lost
        // between frames, even if multiple mouseMoved events fire per frame.
        double[]    mouseDelta    = {0.0, 0.0};   // [dx, dy]
        boolean[]   altDown       = {false};
        int[]       lastCursorX   = {canvas.getWidth() / 2};
        int[]       lastCursorY   = {canvas.getHeight() / 2};
        boolean[]   f5WasDown     = {false};
        boolean[]   inspecting    = {false};

        Robot robot;
        try { robot = new Robot(); } catch (Exception e) { throw new RuntimeException("Failed to create AWT Robot", e); }
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank");

        // Capture / release helpers
        Runnable captureCursor = () -> {
            if (!lookCaptured[0]) {
                lookCaptured[0] = true;
                // Re-center immediately so the first delta is zero
                try {
                    Point screen = canvas.getLocationOnScreen();
                    robot.mouseMove(screen.x + canvas.getWidth() / 2,
                                    screen.y + canvas.getHeight() / 2);
                } catch (Exception ignored) {}
                synchronized (mouseDelta) { mouseDelta[0] = 0; mouseDelta[1] = 0; }
                canvas.setCursor(blankCursor);
            }
        };
        Runnable releaseCursor = () -> {
            if (lookCaptured[0]) {
                lookCaptured[0] = false;
                synchronized (mouseDelta) { mouseDelta[0] = 0; mouseDelta[1] = 0; }
                canvas.setCursor(Cursor.getDefaultCursor());
            }
        };

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Always track cursor for inspect-mode ray
                lastCursorX[0] = e.getX();
                lastCursorY[0] = e.getY();

                if (!lookCaptured[0]) return;

                int cx = canvas.getWidth()  / 2;
                int cy = canvas.getHeight() / 2;
                int dx = e.getX() - cx;
                int dy = e.getY() - cy;

                // Ignore robot re-center events: position is within 1 px of centre.
                // We check by distance rather than a flag so concurrent events
                // (real move + robot move arriving out of order) don't corrupt the state.
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) return;

                // Accumulate — do NOT overwrite; multiple events may fire per frame.
                synchronized (mouseDelta) {
                    mouseDelta[0] += dx;
                    mouseDelta[1] += dy;
                }

                // Re-center cursor to keep it inside the window
                try {
                    Point screen = canvas.getLocationOnScreen();
                    robot.mouseMove(screen.x + cx, screen.y + cy);
                } catch (Exception ignored) {}
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (inspecting[0]) return;
                if (!lookCaptured[0]) {
                    captureCursor.run();
                    canvas.requestFocusInWindow();
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) input.setPressed(InputActions.BREAK_BLOCK, true);
                if (e.getButton() == MouseEvent.BUTTON3) input.setPressed(InputActions.PLACE_BLOCK, true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) input.setPressed(InputActions.BREAK_BLOCK, false);
                if (e.getButton() == MouseEvent.BUTTON3) input.setPressed(InputActions.PLACE_BLOCK, false);
            }
        });

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                boolean ctrl = (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0;
                altDown[0] = (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ALT -> altDown[0] = true;

                    // WASD / movement — only when playing
                    case KeyEvent.VK_W -> { if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.MOVE_FORWARD, true); }
                    case KeyEvent.VK_S -> { if (!ctrl && uiState[0] == GameState.PLAYING) input.setPressed(InputActions.MOVE_BACK, true); }
                    case KeyEvent.VK_A -> { if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.MOVE_LEFT,  true); }
                    case KeyEvent.VK_D -> { if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.MOVE_RIGHT, true); }
                    case KeyEvent.VK_SPACE -> { if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.JUMP,   true); }
                    case KeyEvent.VK_SHIFT -> { if (uiState[0] == GameState.PLAYING) input.setPressed(InputActions.SPRINT, true); }

                    case KeyEvent.VK_F5 -> {
                        if (!f5WasDown[0] && uiState[0] == GameState.PLAYING) {
                            f5WasDown[0] = true;
                            cameraSystem.setMode(cameraSystem.mode() == CameraMode.FIRST_PERSON
                                    ? CameraMode.THIRD_PERSON : CameraMode.FIRST_PERSON);
                        }
                    }

                    case KeyEvent.VK_T -> {
                        if (uiState[0] == GameState.PLAYING) {
                            uiState[0] = GameState.CHAT;
                            clearMovementInput(input);
                            releaseCursor.run();
                        }
                    }

                    case KeyEvent.VK_ESCAPE -> {
                        switch (uiState[0]) {
                            case PLAYING -> {
                                uiState[0] = GameState.PAUSED;
                                menuSel[0] = com.terralite.runtime.render.PauseMenuOverlay.Item.RESUME;
                                clearMovementInput(input);
                                releaseCursor.run();
                            }
                            case PAUSED -> {
                                uiState[0] = GameState.PLAYING;
                                captureCursor.run();
                            }
                            case CHAT -> {
                                chatInput.setLength(0);
                                uiState[0] = GameState.PLAYING;
                                captureCursor.run();
                            }
                        }
                    }

                    case KeyEvent.VK_UP -> {
                        if (uiState[0] == GameState.PAUSED) {
                            var items = com.terralite.runtime.render.PauseMenuOverlay.Item.values();
                            menuSel[0] = items[(menuSel[0].ordinal() - 1 + items.length) % items.length];
                        }
                    }
                    case KeyEvent.VK_DOWN -> {
                        if (uiState[0] == GameState.PAUSED) {
                            var items = com.terralite.runtime.render.PauseMenuOverlay.Item.values();
                            menuSel[0] = items[(menuSel[0].ordinal() + 1) % items.length];
                        }
                    }

                    case KeyEvent.VK_ENTER -> {
                        if (uiState[0] == GameState.PAUSED) {
                            switch (menuSel[0]) {
                                case RESUME -> { uiState[0] = GameState.PLAYING; captureCursor.run(); }
                                case QUIT   -> shutdownSignal.set(true);
                            }
                        } else if (uiState[0] == GameState.CHAT) {
                            String msg = chatInput.toString().trim();
                            if (!msg.isEmpty()) {
                                chatMessages.add(msg);
                                System.out.println("[Chat] " + msg);
                            }
                            chatInput.setLength(0);
                        }
                    }

                    case KeyEvent.VK_BACK_SPACE -> {
                        if (uiState[0] == GameState.CHAT && chatInput.length() > 0) {
                            chatInput.deleteCharAt(chatInput.length() - 1);
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                altDown[0] = (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ALT   -> altDown[0] = false;
                    case KeyEvent.VK_W     -> input.setPressed(InputActions.MOVE_FORWARD, false);
                    case KeyEvent.VK_S     -> input.setPressed(InputActions.MOVE_BACK,    false);
                    case KeyEvent.VK_A     -> input.setPressed(InputActions.MOVE_LEFT,    false);
                    case KeyEvent.VK_D     -> input.setPressed(InputActions.MOVE_RIGHT,   false);
                    case KeyEvent.VK_SPACE -> input.setPressed(InputActions.JUMP,         false);
                    case KeyEvent.VK_SHIFT -> input.setPressed(InputActions.SPRINT,       false);
                    case KeyEvent.VK_F5    -> f5WasDown[0] = false;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (uiState[0] == GameState.CHAT
                        && c != KeyEvent.CHAR_UNDEFINED
                        && !Character.isISOControl(c)
                        && chatInput.length() < 40) {
                    chatInput.append(c);
                }
            }
        });

        try {
            renderer.start();
            captureCursor.run();
            canvas.requestFocusInWindow();

            pipelineRef[0] = new RenderPipeline(
                    world, camera, renderer, window.viewport(), SKY_COLOR,
                    content.gameData(), modelMeshes, textureAtlas);

            GameSaveSystem saveSystem = new GameSaveSystem(world, player.id(), camera, pipelineRef[0])
                    .onLoad(terrainSystem::reset);

            long   lastNanos = System.nanoTime();
            double walkTime  = 0.0;
            double totalTime = 0.0;
            BlockRaycaster.HitResult[] lastInspectionTarget = {null};

            while (!backend.shouldClose()) {
                long frameStart = System.nanoTime();

                boolean uiOpen = uiState[0] != GameState.PLAYING;

                // Alt-held inspection mode
                boolean shouldInspect = !uiOpen && altDown[0];
                if (shouldInspect != inspecting[0]) {
                    inspecting[0] = shouldInspect;
                    input.setPressed(InputActions.BREAK_BLOCK, false);
                    input.setPressed(InputActions.PLACE_BLOCK, false);
                    if (shouldInspect) {
                        releaseCursor.run();
                    } else {
                        blockInteraction.setInspectionRay(null);
                        pipelineRef[0].setInspectionTooltip(List.of());
                        lastInspectionTarget[0] = null;
                        if (!uiOpen) captureCursor.run();
                    }
                }

                if (inspecting[0]) {
                    blockInteraction.setInspectionRay(canvasCursorRay(canvas,
                            lastCursorX[0], lastCursorY[0], camera));
                    blockInteraction.refreshTarget(world);
                } else {
                    blockInteraction.setInspectionRay(null);
                }

                // Drain accumulated mouse delta and apply to camera
                if (lookCaptured[0]) {
                    double dx, dy;
                    synchronized (mouseDelta) {
                        dx = mouseDelta[0];
                        dy = mouseDelta[1];
                        mouseDelta[0] = 0;
                        mouseDelta[1] = 0;
                    }
                    if (dx != 0 || dy != 0) {
                        final double SENSITIVITY = 0.12;
                        camera.setYaw(camera.yaw() - dx * SENSITIVITY);
                        camera.setPitch(Math.max(-89.0, Math.min(89.0,
                                camera.pitch() - dy * SENSITIVITY)));
                    }
                }

                long now = System.nanoTime();
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                totalTime += dt;
                boolean moving = input.isPressed(com.terralite.engine.input.InputActions.MOVE_FORWARD)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_BACK)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_LEFT)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_RIGHT);
                if (moving) walkTime += dt;

                if (uiState[0] != GameState.PAUSED) {
                    engine.advance((long)(dt * 1_000_000_000.0));
                }

                if (inspecting[0]) {
                    BlockRaycaster.HitResult target = blockInteraction.currentTarget();
                    if (!java.util.Objects.equals(lastInspectionTarget[0], target)) {
                        lastInspectionTarget[0] = target;
                        if (target == null) {
                            pipelineRef[0].setInspectionTooltip(List.of("No block"));
                        } else {
                            pipelineRef[0].setInspectionTooltip(
                                    BlockInspectionInfo.lines(world, content.gameData(), target.blockPos()));
                        }
                    }
                }

                if (cameraSystem.mode() == com.terralite.launcher.camera.CameraMode.THIRD_PERSON) {
                    var pt = player.require(com.terralite.engine.physics.PhysicsComponents.TRANSFORM);
                    com.terralite.engine.physics.Collider _collider = player.get(com.terralite.engine.physics.PhysicsComponents.COLLIDER);
                    double halfH = _collider != null ? _collider.halfHeight() : 0.9;
                    com.terralite.runtime.render.AnimationPose selfPose = moving
                            ? com.terralite.runtime.render.PlayerAnimations.walk(walkTime)
                            : com.terralite.runtime.render.PlayerAnimations.idle(totalTime);
                    pipelineRef[0].setPlayerMeshes(java.util.List.of(
                            com.terralite.runtime.render.PlayerBoxBuilder.build(
                                    "_self", pt.x(), pt.y() - halfH, pt.z(),
                                    camera.yaw(), camera.pitch(), playerModel, selfPose)));
                } else {
                    pipelineRef[0].setPlayerMeshes(java.util.List.of());
                }

                pipelineRef[0].setViewport(window.viewport());
                pipelineRef[0].setSelection(blockInteraction.currentTarget());

                List<com.terralite.render.RenderChunkMesh> uiMeshes = new ArrayList<>();
                if (uiState[0] == GameState.PAUSED) {
                    uiMeshes.add(com.terralite.runtime.render.PauseMenuOverlay.build(
                            camera, pipelineRef[0].viewport(), menuSel[0]));
                }
                boolean chatVisible = uiState[0] == GameState.CHAT || !chatMessages.isEmpty();
                if (chatVisible) {
                    uiMeshes.add(com.terralite.runtime.render.ChatOverlay.build(
                            camera, pipelineRef[0].viewport(),
                            chatMessages, chatInput.toString(),
                            uiState[0] == GameState.CHAT));
                }
                pipelineRef[0].setUiOverlays(uiMeshes);

                pipelineRef[0].renderFrame();

                long elapsed = System.nanoTime() - frameStart;
                long sleepNanos = TARGET_FRAME_NANOS - elapsed;
                if (sleepNanos > 1_000_000L) {
                    Thread.sleep(sleepNanos / 1_000_000L);
                }
            }
        } finally {
            terrainSystem.shutdown();
            engine.stop();
            if (pipelineRef[0] != null) pipelineRef[0].shutdown();
            renderer.stop();
        }
    }

    private static void clearMovementInput(InputState input) {
        input.setPressed(InputActions.MOVE_FORWARD, false);
        input.setPressed(InputActions.MOVE_BACK,    false);
        input.setPressed(InputActions.MOVE_LEFT,    false);
        input.setPressed(InputActions.MOVE_RIGHT,   false);
        input.setPressed(InputActions.JUMP,         false);
    }

    private static BlockInteractionSystem.Ray canvasCursorRay(Canvas canvas, int cursorX, int cursorY, Camera camera) {
        double windowWidth  = Math.max(1, canvas.getWidth());
        double windowHeight = Math.max(1, canvas.getHeight());
        double ndcX = (cursorX / windowWidth)  * 2.0 - 1.0;
        double ndcY = 1.0 - (cursorY / windowHeight) * 2.0;

        double yawRad   = Math.toRadians(camera.yaw());
        double pitchRad = Math.toRadians(camera.pitch());
        double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double forwardY =  Math.sin(pitchRad);
        double forwardZ = -Math.cos(yawRad) * Math.cos(pitchRad);
        double rightX   =  Math.cos(yawRad);
        double rightY   = 0.0;
        double rightZ   = -Math.sin(yawRad);
        double upX = rightY * forwardZ - rightZ * forwardY;
        double upY = rightZ * forwardX - rightX * forwardZ;
        double upZ = rightX * forwardY - rightY * forwardX;
        double tanHalfFov = Math.tan(Math.toRadians(camera.fovDegrees()) * 0.5);
        double aspect = windowWidth / windowHeight;
        double dirX = forwardX + rightX * ndcX * tanHalfFov * aspect + upX * ndcY * tanHalfFov;
        double dirY = forwardY + rightY * ndcX * tanHalfFov * aspect + upY * ndcY * tanHalfFov;
        double dirZ = forwardZ + rightZ * ndcX * tanHalfFov * aspect + upZ * ndcY * tanHalfFov;
        Transform transform = camera.transform();
        return new BlockInteractionSystem.Ray(transform.x(), transform.y(), transform.z(), dirX, dirY, dirZ);
    }

    private static boolean isAltDown(long windowHandle) {
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    private static void setWindowTitle(long windowHandle, String[] lastWindowTitle, String title) {
        if (!title.equals(lastWindowTitle[0])) {
            GLFW.glfwSetWindowTitle(windowHandle, title);
            lastWindowTitle[0] = title;
        }
    }

    private static BlockInteractionSystem.Ray cursorRay(long windowHandle, Camera camera) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var cursorX = stack.mallocDouble(1);
            var cursorY = stack.mallocDouble(1);
            var width = stack.mallocInt(1);
            var height = stack.mallocInt(1);
            GLFW.glfwGetCursorPos(windowHandle, cursorX, cursorY);
            GLFW.glfwGetWindowSize(windowHandle, width, height);

            double windowWidth = Math.max(1, width.get(0));
            double windowHeight = Math.max(1, height.get(0));
            double ndcX = (cursorX.get(0) / windowWidth) * 2.0 - 1.0;
            double ndcY = 1.0 - (cursorY.get(0) / windowHeight) * 2.0;

            double yawRad = Math.toRadians(camera.yaw());
            double pitchRad = Math.toRadians(camera.pitch());
            double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
            double forwardY = Math.sin(pitchRad);
            double forwardZ = -Math.cos(yawRad) * Math.cos(pitchRad);

            double rightX = Math.cos(yawRad);
            double rightY = 0.0;
            double rightZ = -Math.sin(yawRad);

            double upX = rightY * forwardZ - rightZ * forwardY;
            double upY = rightZ * forwardX - rightX * forwardZ;
            double upZ = rightX * forwardY - rightY * forwardX;

            double tanHalfFov = Math.tan(Math.toRadians(camera.fovDegrees()) * 0.5);
            double aspect = windowWidth / windowHeight;
            double dirX = forwardX + rightX * ndcX * tanHalfFov * aspect + upX * ndcY * tanHalfFov;
            double dirY = forwardY + rightY * ndcX * tanHalfFov * aspect + upY * ndcY * tanHalfFov;
            double dirZ = forwardZ + rightZ * ndcX * tanHalfFov * aspect + upZ * ndcY * tanHalfFov;

            Transform transform = camera.transform();
            return new BlockInteractionSystem.Ray(transform.x(), transform.y(), transform.z(), dirX, dirY, dirZ);
        }
    }
}
