package com.terralite.runtime.render;

import com.terralite.core.registry.GameData;
import com.terralite.content.assets.model.ContentModelMesh;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.camera.Camera;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.terrain.BlockRaycaster;
import com.terralite.engine.world.World;
import com.terralite.render.math.CameraMatrices;
import com.terralite.render.math.FrustumCuller;
import com.terralite.render.ClearColor;
import com.terralite.render.RenderCamera;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderScene;
import com.terralite.render.RenderStats;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;
import com.terralite.render.texture.TextureAtlas;
import com.terralite.render.texture.TextureAtlasMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class RenderPipeline {
    private final World world;
    private final Camera camera;
    private final Renderer renderer;
    private final GameData gameData;
    private final Map<ResourceId, ContentModelMesh> modelMeshes;
    private final TextureAtlas textureAtlas;
    private final ChunkMeshBuilder chunkMeshBuilder;

    // Stores atlas-remapped meshes — rebuilt only when a chunk is dirty.
    // Keyed by ChunkPos; value is empty if the chunk has no visible faces.
    private final Map<ChunkPos, Optional<RenderChunkMesh>> meshCache = new HashMap<>();
    private final FrustumCuller frustum = new FrustumCuller();
    private BlockRaycaster.HitResult selection = null;
    private final Set<ChunkPos> dirtyChunks = new HashSet<>();
    private Set<ChunkPos> lastKnownChunks = Set.of();

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
        this.chunkMeshBuilder = gameData != null
                ? new ChunkMeshBuilder(gameData, this.modelMeshes)
                : new ChunkMeshBuilder();
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

    /** Updates the currently targeted block face for highlight rendering. Pass {@code null} to clear. */
    public void setSelection(BlockRaycaster.HitResult hit) {
        this.selection = hit;
    }

    /** Clears all cached chunk meshes, forcing a full rebuild on the next frame (e.g. after world load). */
    public void clearMeshCache() {
        meshCache.clear();
        dirtyChunks.clear();
        lastKnownChunks = Set.of();
    }

    private Optional<RenderChunkMesh> remap(Optional<RenderChunkMesh> raw) {
        if (raw.isEmpty() || textureAtlas == null) return raw;
        return Optional.of(new RenderChunkMesh(
                raw.get().chunk(),
                new TextureAtlasMapper().remap(raw.get().mesh(), textureAtlas)));
    }

    /** Mark a chunk as needing a mesh rebuild on the next frame (e.g. after a block change). */
    public void markDirty(ChunkPos pos) {
        dirtyChunks.add(Objects.requireNonNull(pos, "pos"));
    }

    public RenderStats renderFrame() {
        syncChunkCache();

        RenderCamera renderCamera = RenderSceneExtractor.toRenderCamera(camera);
        RenderScene.Builder scene = RenderScene.builder().camera(renderCamera);

        // Submit all cached meshes — buffer lifecycle is tied to chunk load, not visibility.
        // Frustum culling happens in the GPU command buffer, not here.
        world.chunkPositions().stream()
                .sorted(Comparator.comparingInt(ChunkPos::x)
                        .thenComparingInt(ChunkPos::y)
                        .thenComparingInt(ChunkPos::z))
                .forEach(pos -> {
                    scene.addChunk(new RenderChunk(pos.x(), pos.y(), pos.z()));
                    Optional<RenderChunkMesh> mesh = meshCache.get(pos);
                    if (mesh != null) mesh.ifPresent(scene::addChunkMesh);
                });

        // Add entity objects
        world.entities().entities().stream()
                .sorted(Comparator.comparingLong(e -> e.id().value()))
                .forEach(entity -> RenderSceneExtractor.addEntityObject(scene, entity));

        // Meshes are already atlas-remapped in the cache — no per-frame remapping needed.
        if (selection != null) {
            SelectionHighlightBuilder.build(selection).ifPresent(scene::addChunkMesh);
        }

        RenderScene result = scene.build();
        return textureAtlas != null
                ? renderer.render(new RenderFrame(viewport, clearColor, result, textureAtlas))
                : renderer.render(new RenderFrame(viewport, clearColor, result));
    }

    private void syncChunkCache() {
        Collection<ChunkPos> currentChunks = world.chunkPositions();

        // Evict chunks that no longer exist
        Set<ChunkPos> removed = new HashSet<>(lastKnownChunks);
        removed.removeAll(currentChunks);
        removed.forEach(meshCache::remove);

        // Build AND atlas-remap meshes for new or dirty chunks (once per change, not per frame)
        for (ChunkPos pos : currentChunks) {
            if (!meshCache.containsKey(pos) || dirtyChunks.contains(pos)) {
                Optional<RenderChunkMesh> raw = chunkMeshBuilder.build(world, new RenderChunk(pos.x(), pos.y(), pos.z()));
                meshCache.put(pos, remap(raw));
            }
        }

        dirtyChunks.clear();
        lastKnownChunks = Set.copyOf(currentChunks);
    }
}
