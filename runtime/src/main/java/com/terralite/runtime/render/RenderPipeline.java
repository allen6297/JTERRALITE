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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class RenderPipeline {
    private static final int MAX_CHUNK_MESH_REBUILDS_PER_FRAME = 1;

    private final World world;
    private final Camera camera;
    private final Renderer renderer;
    private final GameData gameData;
    private final Map<ResourceId, ContentModelMesh> modelMeshes;
    private final TextureAtlas textureAtlas;
    private final ChunkMeshBuilder chunkMeshBuilder;
    private final ExecutorService meshExecutor;

    // Stores atlas-remapped meshes — rebuilt only when a chunk is dirty.
    // Keyed by ChunkPos; value is empty if the chunk has no visible faces.
    private final Map<ChunkPos, Optional<RenderChunkMesh>> meshCache = new HashMap<>();
    private final FrustumCuller frustum = new FrustumCuller();
    private BlockRaycaster.HitResult selection = null;
    private Optional<RenderChunkMesh> selectionMesh = Optional.empty();
    private List<String> inspectionTooltipLines = List.of();
    private Optional<RenderChunkMesh> inspectionTooltipMesh = Optional.empty();
    private final Set<ChunkPos> dirtyChunks = new HashSet<>();
    private final Map<ChunkPos, Future<MeshBuildResult>> meshJobs = new HashMap<>();
    private final Map<ChunkPos, Long> meshVersions = new HashMap<>();
    private long nextMeshVersion = 1L;
    private Set<ChunkPos> lastKnownChunks = Set.of();

    private Viewport viewport;
    private ClearColor clearColor;
    private List<RenderChunkMesh> playerMeshes = List.of();
    private List<RenderChunkMesh> uiOverlays   = List.of();

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
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
        this.meshExecutor = Executors.newFixedThreadPool(threads, r -> {
            Thread thread = new Thread(r, "chunk-mesh");
            thread.setDaemon(true);
            return thread;
        });
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

    public void setPlayerMeshes(List<RenderChunkMesh> meshes) {
        if (meshes == null || meshes.isEmpty()) {
            this.playerMeshes = List.of();
            return;
        }
        // Remap UVs to atlas coordinates so skin texture renders correctly
        this.playerMeshes = meshes.stream()
                .map(m -> remap(Optional.of(m)).orElse(m))
                .toList();
    }

    /** Updates the currently targeted block face for highlight rendering. Pass {@code null} to clear. */
    public void setSelection(BlockRaycaster.HitResult hit) {
        if (Objects.equals(selection, hit)) {
            return;
        }
        this.selection = hit;
        this.selectionMesh = hit == null ? Optional.empty() : SelectionHighlightBuilder.build(hit);
    }

    /**
     * Replaces the UI overlay mesh list (pause menu, chat, HUD, etc.).
     * Pass an empty list to clear all overlays.
     */
    public void setUiOverlays(List<RenderChunkMesh> overlays) {
        if (overlays == null || overlays.isEmpty()) {
            this.uiOverlays = List.of();
            return;
        }
        // Remap per-texture UVs to atlas coordinates, same as inspectionTooltipMesh / playerMeshes.
        this.uiOverlays = overlays.stream()
                .map(m -> remap(Optional.of(m)).orElse(m))
                .toList();
    }

    public void setInspectionTooltip(List<String> lines) {
        List<String> next = lines == null ? List.of() : List.copyOf(lines);
        if (inspectionTooltipLines.equals(next)) {
            return;
        }
        this.inspectionTooltipLines = next;
        this.inspectionTooltipMesh = remap(InspectionTooltipBuilder.build(camera, viewport, next));
    }

    /** Clears all cached chunk meshes, forcing a full rebuild on the next frame (e.g. after world load). */
    public void clearMeshCache() {
        meshCache.clear();
        dirtyChunks.clear();
        cancelMeshJobs();
        meshVersions.clear();
        lastKnownChunks = Set.of();
    }

    public void shutdown() {
        cancelMeshJobs();
        meshExecutor.shutdownNow();
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

    /** Mark a chunk and adjacent chunks dirty so shared faces update across chunk borders. */
    public void markDirtyWithNeighbors(ChunkPos pos) {
        Objects.requireNonNull(pos, "pos");
        markDirty(pos);
        markDirty(ChunkPos.of(pos.x() - 1, pos.y(), pos.z()));
        markDirty(ChunkPos.of(pos.x() + 1, pos.y(), pos.z()));
        markDirty(ChunkPos.of(pos.x(), pos.y() - 1, pos.z()));
        markDirty(ChunkPos.of(pos.x(), pos.y() + 1, pos.z()));
        markDirty(ChunkPos.of(pos.x(), pos.y(), pos.z() - 1));
        markDirty(ChunkPos.of(pos.x(), pos.y(), pos.z() + 1));
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

        // Dynamic meshes (overlays + player models) bypass the terrain upload throttle
        // so their positions always reflect the latest data.
        selectionMesh.ifPresent(scene::addDynamicMesh);
        inspectionTooltipMesh.ifPresent(scene::addDynamicMesh);
        playerMeshes.forEach(scene::addDynamicMesh);
        uiOverlays.forEach(scene::addDynamicMesh);

        RenderScene result = scene.build();
        return textureAtlas != null
                ? renderer.render(new RenderFrame(viewport, clearColor, result, textureAtlas))
                : renderer.render(new RenderFrame(viewport, clearColor, result));
    }

    private void syncChunkCache() {
        Collection<ChunkPos> currentChunks = world.chunkPositions();
        Set<ChunkPos> currentChunkSet = Set.copyOf(currentChunks);

        // Evict chunks that no longer exist
        Set<ChunkPos> removed = new HashSet<>(lastKnownChunks);
        removed.removeAll(currentChunks);
        removed.forEach(meshCache::remove);
        dirtyChunks.removeAll(removed);
        removed.forEach(pos -> {
            Future<MeshBuildResult> job = meshJobs.remove(pos);
            if (job != null) job.cancel(true);
            meshVersions.remove(pos);
        });

        for (var entry : List.copyOf(meshJobs.entrySet())) {
            Future<MeshBuildResult> job = entry.getValue();
            if (!job.isDone()) continue;
            meshJobs.remove(entry.getKey());
            try {
                MeshBuildResult result = job.get();
                Long expectedVersion = meshVersions.get(result.pos());
                if (currentChunkSet.contains(result.pos())
                        && expectedVersion != null
                        && expectedVersion == result.version()
                        && !dirtyChunks.contains(result.pos())) {
                    meshCache.put(result.pos(), remap(result.mesh()));
                }
            } catch (java.util.concurrent.CancellationException ignored) {
                // job was cancelled intentionally; do not re-dirty
            } catch (Exception ignored) {
                dirtyChunks.add(entry.getKey());
            }
        }

        for (ChunkPos pos : List.copyOf(dirtyChunks)) {
            Future<MeshBuildResult> job = meshJobs.remove(pos);
            if (job != null) job.cancel(true);
        }

        // Snapshot one chunk per frame on the render thread; CPU mesh construction happens on workers.
        List<ChunkPos> rebuilds = currentChunks.stream()
                .filter(pos -> !meshCache.containsKey(pos) || dirtyChunks.contains(pos))
                .filter(pos -> !meshJobs.containsKey(pos))
                .sorted(Comparator.comparingDouble(this::distanceSquaredToCamera))
                .limit(MAX_CHUNK_MESH_REBUILDS_PER_FRAME)
                .toList();

        for (ChunkPos pos : rebuilds) {
            RenderChunk chunk = new RenderChunk(pos.x(), pos.y(), pos.z());
            ChunkMeshBuilder.ChunkMeshSnapshot snapshot = chunkMeshBuilder.snapshot(world, chunk);
            long version = nextMeshVersion++;
            meshVersions.put(pos, version);
            dirtyChunks.remove(pos);
            meshJobs.put(pos, meshExecutor.submit(() ->
                    new MeshBuildResult(pos, version, chunkMeshBuilder.build(snapshot))));
        }

        lastKnownChunks = currentChunkSet;
    }

    private void cancelMeshJobs() {
        meshJobs.values().forEach(job -> job.cancel(true));
        meshJobs.clear();
    }

    private double distanceSquaredToCamera(ChunkPos pos) {
        double chunkCenterX = (pos.x() + 0.5) * ChunkMeshBuilder.CHUNK_SIZE;
        double chunkCenterY = (pos.y() + 0.5) * ChunkMeshBuilder.CHUNK_SIZE;
        double chunkCenterZ = (pos.z() + 0.5) * ChunkMeshBuilder.CHUNK_SIZE;
        var transform = camera.transform();
        double dx = chunkCenterX - transform.x();
        double dy = chunkCenterY - transform.y();
        double dz = chunkCenterZ - transform.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private record MeshBuildResult(ChunkPos pos, long version, Optional<RenderChunkMesh> mesh) {
        private MeshBuildResult {
            Objects.requireNonNull(pos, "pos");
            Objects.requireNonNull(mesh, "mesh");
        }
    }
}
