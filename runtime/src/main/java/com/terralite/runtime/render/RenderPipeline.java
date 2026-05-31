package com.terralite.runtime.render;

import com.terralite.core.registry.GameData;
import com.terralite.content.assets.model.ContentModelMesh;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.camera.Camera;
import com.terralite.engine.world.World;
import com.terralite.render.ClearColor;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderScene;
import com.terralite.render.RenderStats;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;
import com.terralite.render.texture.TextureAtlas;
import com.terralite.render.texture.TextureAtlasMapper;

import java.util.Objects;
import java.util.Map;

public final class RenderPipeline {
    private final World world;
    private final Camera camera;
    private final Renderer renderer;
    private final GameData gameData;
    private final Map<ResourceId, ContentModelMesh> modelMeshes;
    private final TextureAtlas textureAtlas;
    private Viewport viewport;
    private ClearColor clearColor;

    public RenderPipeline(World world, Camera camera, Renderer renderer, Viewport viewport, ClearColor clearColor) {
        this(world, camera, renderer, viewport, clearColor, null);
    }

    public RenderPipeline(
            World world,
            Camera camera,
            Renderer renderer,
            Viewport viewport,
            ClearColor clearColor,
            GameData gameData
    ) {
        this(world, camera, renderer, viewport, clearColor, gameData, Map.of(), null);
    }

    public RenderPipeline(
            World world,
            Camera camera,
            Renderer renderer,
            Viewport viewport,
            ClearColor clearColor,
            GameData gameData,
            Map<ResourceId, ContentModelMesh> modelMeshes
    ) {
        this(world, camera, renderer, viewport, clearColor, gameData, modelMeshes, null);
    }

    public RenderPipeline(
            World world,
            Camera camera,
            Renderer renderer,
            Viewport viewport,
            ClearColor clearColor,
            GameData gameData,
            Map<ResourceId, ContentModelMesh> modelMeshes,
            TextureAtlas textureAtlas
    ) {
        this.world = Objects.requireNonNull(world, "world");
        this.camera = Objects.requireNonNull(camera, "camera");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.clearColor = Objects.requireNonNull(clearColor, "clearColor");
        this.gameData = gameData;
        this.modelMeshes = Map.copyOf(Objects.requireNonNull(modelMeshes, "modelMeshes"));
        this.textureAtlas = textureAtlas;
    }

    public World world() {
        return world;
    }

    public Camera camera() {
        return camera;
    }

    public Renderer renderer() {
        return renderer;
    }

    public Viewport viewport() {
        return viewport;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = Objects.requireNonNull(viewport, "viewport");
    }

    public ClearColor clearColor() {
        return clearColor;
    }

    public void setClearColor(ClearColor clearColor) {
        this.clearColor = Objects.requireNonNull(clearColor, "clearColor");
    }

    public RenderStats renderFrame() {
        RenderScene scene = gameData == null
                ? RenderSceneExtractor.from(world, camera)
                : RenderSceneExtractor.from(world, camera, gameData, modelMeshes);
        if (textureAtlas == null || scene.chunkMeshes().isEmpty()) {
            return renderer.render(new RenderFrame(viewport, clearColor, scene));
        }
        RenderScene remappedScene = RenderScene.builder()
                .camera(scene.camera())
                .addChunks(scene.chunks())
                .addChunkMeshes(new TextureAtlasMapper().remap(scene.chunkMeshes(), textureAtlas))
                .addObjects(scene.objects())
                .build();
        return renderer.render(new RenderFrame(viewport, clearColor, remappedScene, textureAtlas));
    }
}
