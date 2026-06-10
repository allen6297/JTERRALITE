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
import com.terralite.engine.physics.*;
import com.terralite.engine.player.PlayerControlled;
import com.terralite.engine.player.YawRelativePlayerInputSystem;
import com.terralite.engine.world.World;
import com.terralite.game.content.GameContentLoadReport;
import com.terralite.game.content.GameContentLoader;
import com.terralite.launcher.camera.CameraMode;
import com.terralite.launcher.camera.PlayerCameraSystem;
import com.terralite.launcher.input.MouseLookController;
import com.terralite.launcher.net.NetworkClient;
import com.terralite.render.ClearColor;
import com.terralite.render.Renderer;
import com.terralite.render.backend.VulkanRenderBackend;
import com.terralite.render.glfw.GlfwWindow;
import com.terralite.render.texture.TextureAtlasBuilder;
import com.terralite.render.window.WindowConfig;
import com.terralite.core.registry.ResourceId;
import com.terralite.render.RenderChunkMesh;
import com.terralite.runtime.interaction.BlockInteractionSystem;
import com.terralite.content.assets.model.ContentModelMesh;
import com.terralite.runtime.render.PlayerAssetInstaller;
import com.terralite.runtime.render.PlayerBoxBuilder;
import com.terralite.runtime.render.RenderPipeline;
import com.terralite.runtime.world.RuntimeWorldFactory;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import com.terralite.core.logging.Loggers;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.terralite.runtime.render.AnimationPose;
import com.terralite.runtime.render.PlayerAnimations;

/**
 * Game client that connects to a remote {@link NetworkServer} for shared world state.
 *
 * <p>Physics and input run locally (instant feedback). Chunks arrive from the server;
 * local terrain generation is skipped entirely. Block interactions are applied locally
 * for responsiveness and forwarded to the server for propagation to other clients.
 */
final class RemoteClientApp {
    private static final Logger log = Loggers.get(RemoteClientApp.class);

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final double PLAYER_SPEED = 5.0;
    private static final long TARGET_FRAME_NANOS = 1_000_000_000L / 60;
    private static final ClearColor SKY_COLOR = new ClearColor(0.45f, 0.65f, 0.85f, 1.0f);

    private final Path packsRoot;
    private final String serverHost;
    private final int serverPort;

    RemoteClientApp(Path packsRoot, String serverHost, int serverPort) {
        this.packsRoot = Objects.requireNonNull(packsRoot, "packsRoot");
        this.serverHost = Objects.requireNonNull(serverHost, "serverHost");
        this.serverPort = serverPort;
    }

    void run() throws Exception {
        PlayerAssetInstaller.bootstrap(packsRoot);
        GameContentLoadReport content = new GameContentLoader().load(packsRoot);
        log.info("Loaded {} content pack(s)", content.packs().size());

        var assets = ContentAssetIndex.load(content.packs());
        var modelMeshes = new ContentModelMeshLibrary()
                .loadSupported(ContentModelIndex.load(content.packs()));
        var textureAtlas = new TextureAtlasBuilder().build(
                assets.assets().stream()
                        .filter(a -> a.type().equals("textures"))
                        .collect(Collectors.toMap(a -> a.id(), a -> a.path())));

        // World is populated by the server — no terrain system
        World world = new RuntimeWorldFactory().createEmpty(content.gameData());

        RenderPipeline[] pipelineRef = {null};

        Map<String, RenderChunkMesh> playerMeshes = new ConcurrentHashMap<>();
        ContentModelMesh playerModel = modelMeshes.get(PlayerBoxBuilder.MODEL_ID);

        // Per-player animation state: walk time (advances when moving) and last known position
        Map<String, double[]> playerAnimState = new ConcurrentHashMap<>();
        // double[0]=walkTime, double[1]=lastX, double[2]=lastZ

        NetworkClient networkClient = new NetworkClient(serverHost, serverPort)
                .onChunkReady(pos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirtyWithNeighbors(pos); })
                .onBlockChange(pos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirtyWithNeighbors(pos); })
                .onPlayerJoin(msg -> {
                    playerAnimState.put(msg.playerId(), new double[]{0.0, msg.x(), msg.z()});
                    playerMeshes.put(msg.playerId(),
                            PlayerBoxBuilder.build(msg.playerId(), msg.x(), msg.y(), msg.z(),
                                    msg.yaw(), msg.pitch(), playerModel, AnimationPose.IDENTITY));
                })
                .onPlayerLeave(msg -> {
                    playerMeshes.remove(msg.playerId());
                    playerAnimState.remove(msg.playerId());
                })
                .onPlayerPosition(msg -> {
                    double[] state = playerAnimState.computeIfAbsent(msg.playerId(),
                            id -> new double[]{0.0, msg.x(), msg.z()});
                    // Advance walkTime based on XZ displacement since last update
                    double dx = msg.x() - state[1];
                    double dz = msg.z() - state[2];
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    boolean moving = dist > 0.01;
                    if (moving) state[0] += dist * 0.5; // scale distance → walk phase
                    state[1] = msg.x();
                    state[2] = msg.z();
                    AnimationPose pose = moving
                            ? PlayerAnimations.walk(state[0])
                            : AnimationPose.IDENTITY;
                    playerMeshes.put(msg.playerId(),
                            PlayerBoxBuilder.build(msg.playerId(), msg.x(), msg.y(), msg.z(),
                                    msg.yaw(), msg.pitch(), playerModel, pose));
                });
        networkClient.connect();
        log.info("Connected to {}:{}", serverHost, serverPort);

        Camera camera = new Camera(new Transform(0.0, 5.0, 0.0), 70.0, 0.1, 1000.0);
        InputState input = new InputState();

        Entity player = world.spawner().spawn(EntitySpawnRequest.create()
                .transform(new Transform(8.0, 5.0, 8.0))
                .playerControlled(new PlayerControlled(PLAYER_SPEED)));
        player.set(PhysicsComponents.COLLIDER, new Collider(0.3, 0.9, 0.3));

        var cameraSystem = new PlayerCameraSystem(camera, player.id(), CameraMode.FIRST_PERSON);

        var blockInteraction = new BlockInteractionSystem(
                input, camera, player.id(), RuntimeWorldFactory.DEFAULT_SURFACE_BLOCK,
                pos -> { if (pipelineRef[0] != null) pipelineRef[0].markDirtyWithNeighbors(pos); })
            .onBlockPlaced((pos, state) -> networkClient.placeBlock(pos, state))
            .onBlockBroken(pos -> networkClient.breakBlock(pos));

        GameEngine engine = GameEngine.builder()
                .world(world)
                .input(input)
                .addWorldSimulationSystem(new YawRelativePlayerInputSystem(input, camera))
                .addWorldSimulationSystem(new GravitySystem())
                .addWorldSimulationSystem(new MovementSystem())
                .addWorldSimulationSystem(new BlockCollisionResolutionSystem())
                .addWorldSimulationSystem(blockInteraction)
                .addWorldSimulationSystem(cameraSystem)
                .tickDeltaMillis(50)
                .build();
        engine.start();

        GlfwWindow window = new GlfwWindow(WindowConfig.vulkan("TERRALITE [multiplayer]", WIDTH, HEIGHT));
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

            GLFW.glfwSetKeyCallback(window.handle(), (win, key, scancode, action, mods) -> {
                boolean pressed = action != GLFW.GLFW_RELEASE;
                switch (key) {
                    case GLFW.GLFW_KEY_W          -> input.setPressed(InputActions.MOVE_FORWARD, pressed);
                    case GLFW.GLFW_KEY_S          -> input.setPressed(InputActions.MOVE_BACK,    pressed);
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
            double selfWalkTime  = 0.0;
            double selfTotalTime = 0.0;
            while (!backend.shouldClose()) {
                long frameStart = System.nanoTime();
                window.pollEvents();

                // Apply pending world updates from server before physics tick
                networkClient.applyPending(world);

                long now = System.nanoTime();
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                selfTotalTime += dt;
                boolean selfMoving = input.isPressed(com.terralite.engine.input.InputActions.MOVE_FORWARD)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_BACK)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_LEFT)
                        || input.isPressed(com.terralite.engine.input.InputActions.MOVE_RIGHT);
                if (selfMoving) selfWalkTime += dt;
                engine.advance((long)(dt * 1_000_000_000.0));

                // Transform is collider center; subtract halfY (0.9) to get feet
                var playerTransform = player.require(PhysicsComponents.TRANSFORM);
                var collider = player.get(PhysicsComponents.COLLIDER);
                double colliderHalfY = collider != null ? collider.halfHeight() : 0.9;
                double feetX = playerTransform.x();
                double feetY = playerTransform.y() - colliderHalfY;
                double feetZ = playerTransform.z();
                networkClient.sendPosition(feetX, feetY, feetZ, camera.yaw(), camera.pitch());

                // Show self model in third-person
                if (cameraSystem.mode() == CameraMode.THIRD_PERSON) {
                    AnimationPose selfPose = selfMoving
                            ? PlayerAnimations.walk(selfWalkTime)
                            : PlayerAnimations.idle(selfTotalTime);
                    playerMeshes.put("_self", PlayerBoxBuilder.build("_self", feetX, feetY, feetZ,
                            camera.yaw(), camera.pitch(), playerModel, selfPose));
                } else {
                    playerMeshes.remove("_self");
                }

                pipelineRef[0].setViewport(window.viewport());
                pipelineRef[0].setSelection(blockInteraction.currentTarget());
                pipelineRef[0].setPlayerMeshes(playerMeshes.values().stream().toList());
                pipelineRef[0].renderFrame();

                long sleepNanos = TARGET_FRAME_NANOS - (System.nanoTime() - frameStart);
                if (sleepNanos > 1_000_000L) Thread.sleep(sleepNanos / 1_000_000L);
            }
        } finally {
            engine.stop();
            if (pipelineRef[0] != null) pipelineRef[0].shutdown();
            networkClient.disconnect();
            renderer.stop();
        }
    }
}
