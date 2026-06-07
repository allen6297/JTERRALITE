package com.terralite.runtime.render;

import com.terralite.core.registry.GameData;
import com.terralite.core.registry.ResourceId;
import com.terralite.content.assets.model.ContentModelMesh;
import com.terralite.content.assets.model.ContentModelVertex;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.world.World;
import com.terralite.game.block.BlockModel;
import com.terralite.game.block.BlockModelVariant;
import com.terralite.game.block.BlockStateRegistry;
import com.terralite.game.block.BlockTextures;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ChunkMeshBuilder {
    public static final int CHUNK_SIZE = 16;

    /**
     * Per-face-direction: {dAxis, uAxis, vAxis, normalSign}.
     * dAxis = axis perpendicular to the face (0=X,1=Y,2=Z).
     * normalSign = +1 (face on + side of block) or -1 (face on - side).
     */
    private static final int[][] FACE_AXES = {
        {1, 0, 2, +1},  // UP    (+Y)
        {1, 0, 2, -1},  // DOWN  (-Y)
        {0, 2, 1, +1},  // EAST  (+X)
        {0, 2, 1, -1},  // WEST  (-X)
        {2, 0, 1, +1},  // SOUTH (+Z)
        {2, 0, 1, -1},  // NORTH (-Z)
    };
    private static final BlockTextures.Face[] TEX_FACES = {
        BlockTextures.Face.UP, BlockTextures.Face.DOWN,
        BlockTextures.Face.EAST, BlockTextures.Face.WEST,
        BlockTextures.Face.SOUTH, BlockTextures.Face.NORTH,
    };

    private final Map<ResourceId, BlockTextures> texturesByBlock;
    private final Map<ResourceId, BlockModel> modelsByBlock;
    private final Map<ResourceId, List<BlockModelVariant>> variantsByBlock;
    private final BlockStateRegistry blockStateRegistry;
    private final Map<Integer, RenderModel> renderModelsByStateId;
    private final Map<ResourceId, ContentModelMesh> modelMeshes;

    public ChunkMeshBuilder() {
        this.texturesByBlock = Map.of();
        this.modelsByBlock = Map.of();
        this.variantsByBlock = Map.of();
        this.blockStateRegistry = null;
        this.renderModelsByStateId = Map.of();
        this.modelMeshes = Map.of();
    }

    public ChunkMeshBuilder(GameData gameData) {
        this(gameData, Map.of());
    }

    public ChunkMeshBuilder(GameData gameData, Map<ResourceId, ContentModelMesh> modelMeshes) {
        Objects.requireNonNull(gameData, "gameData");
        this.texturesByBlock = gameData.registry(TerraliteRegistries.BLOCKS).ids().stream()
                .filter(id -> gameData.registry(TerraliteRegistries.BLOCKS).require(id).properties().textures() != null)
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        id -> gameData.registry(TerraliteRegistries.BLOCKS).require(id).properties().textures()
                ));
        this.modelsByBlock = gameData.registry(TerraliteRegistries.BLOCKS).ids().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        id -> gameData.registry(TerraliteRegistries.BLOCKS).require(id).properties().model()
                ));
        this.variantsByBlock = gameData.registry(TerraliteRegistries.BLOCKS).ids().stream()
                .filter(id -> !gameData.registry(TerraliteRegistries.BLOCKS).require(id).properties().modelVariants().isEmpty())
                .collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        id -> gameData.registry(TerraliteRegistries.BLOCKS).require(id).properties().modelVariants()
                ));
        this.blockStateRegistry = BlockStateRegistry.from(gameData);
        this.renderModelsByStateId = this.blockStateRegistry.states().stream()
                .collect(Collectors.toUnmodifiableMap(
                        blockStateRegistry::requireId,
                        this::resolveRenderModel
                ));
        this.modelMeshes = Map.copyOf(Objects.requireNonNull(modelMeshes, "modelMeshes"));
    }

    public Optional<RenderChunkMesh> build(World world, RenderChunk chunk) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(chunk, "chunk");

        int sx = chunk.x() * CHUNK_SIZE;
        int sy = chunk.y() * CHUNK_SIZE;
        int sz = chunk.z() * CHUNK_SIZE;

        List<DebugVertex> vertices = new ArrayList<>();

        // Build the block cache once — used by both model-mesh and greedy paths
        BlockState[] blockCache = buildBlockCache(world, sx, sy, sz);

        // --- Model-mesh blocks (custom geometry, not greedy-mergeable) ---
        if (!modelMeshes.isEmpty()) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_SIZE; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        BlockState state = cacheGet(blockCache, x, y, z);
                        if (state.isAir()) continue;
                        RenderModel rm = renderModelFor(state);
                        ContentModelMesh mesh = modelMeshes.get(rm.model().id());
                        if (mesh == null) continue;
                        float r = colorChannel(state, 0), g = colorChannel(state, 37), b = colorChannel(state, 73);
                        addModelMesh(vertices, BlockPos.of(sx + x, sy + y, sz + z), mesh, r, g, b, rm.textures());
                    }
                }
            }
        }

        // --- Cube blocks: greedy meshing ---
        buildGreedy(blockCache, sx, sy, sz, vertices);

        if (vertices.isEmpty()) return Optional.empty();
        return Optional.of(new RenderChunkMesh(chunk, new DebugMesh(vertices)));
    }

    // -------------------------------------------------------------------------
    // Greedy meshing
    // -------------------------------------------------------------------------

    // Cache dimensions: chunk + 1-block border on each side
    private static final int PAD  = CHUNK_SIZE + 2; // 18
    private static final int PAD2 = PAD * PAD;      // 324

    /** Pre-loads blocks for the chunk + 1-block border into a flat array for O(1) array access. */
    private static BlockState[] buildBlockCache(World world, int sx, int sy, int sz) {
        BlockState[] cache = new BlockState[PAD * PAD2];
        for (int x = -1; x <= CHUNK_SIZE; x++) {
            for (int y = -1; y <= CHUNK_SIZE; y++) {
                for (int z = -1; z <= CHUNK_SIZE; z++) {
                    cache[(x + 1) * PAD2 + (y + 1) * PAD + (z + 1)] =
                            world.getBlock(BlockPos.of(sx + x, sy + y, sz + z));
                }
            }
        }
        return cache;
    }

    private static BlockState cacheGet(BlockState[] cache, int lx, int ly, int lz) {
        return cache[(lx + 1) * PAD2 + (ly + 1) * PAD + (lz + 1)];
    }

    @SuppressWarnings("unchecked")
    private void buildGreedy(BlockState[] blockCache, int sx, int sy, int sz, List<DebugVertex> out) {
        int cs = CHUNK_SIZE;

        int[] start = {sx, sy, sz};

        // Reusable mask arrays
        FaceCell[][] mask = new FaceCell[cs][cs];
        boolean[][] used  = new boolean[cs][cs];

        for (int f = 0; f < 6; f++) {
            int dAxis  = FACE_AXES[f][0];
            int uAxis  = FACE_AXES[f][1];
            int vAxis  = FACE_AXES[f][2];
            int nSign  = FACE_AXES[f][3];
            BlockTextures.Face texFace = TEX_FACES[f];

            // chunk-local coords (lx, ly, lz) mapped to cache
            int[] lp = new int[3];  // block local
            int[] ln = new int[3];  // neighbor local

            for (int d = 0; d < cs; d++) {
                // Build mask for this depth slice
                for (FaceCell[] row : mask) Arrays.fill(row, null);

                for (int u = 0; u < cs; u++) {
                    for (int v = 0; v < cs; v++) {
                        lp[dAxis] = d; lp[uAxis] = u; lp[vAxis] = v;

                        BlockState state = cacheGet(blockCache, lp[0], lp[1], lp[2]);
                        if (state.isAir()) continue;

                        // Skip blocks with custom model meshes (handled separately)
                        RenderModel rm = renderModelFor(state);
                        if (modelMeshes.containsKey(rm.model().id())) continue;

                        // Face visible only if neighbor is air
                        ln[dAxis] = lp[dAxis] + nSign; ln[uAxis] = u; ln[vAxis] = v;
                        if (!cacheGet(blockCache, ln[0], ln[1], ln[2]).isAir()) continue;

                        BlockTextures textures = rm.textures();
                        ResourceId tex = textures != null ? textures.textureFor(texFace) : null;
                        float r = colorChannel(state, 0);
                        float g = colorChannel(state, 37);
                        float b = colorChannel(state, 73);
                        mask[u][v] = new FaceCell(r, g, b, tex);
                    }
                }

                // Greedy merge
                for (boolean[] row : used) Arrays.fill(row, false);

                int faceDepth = start[dAxis] + d + (nSign > 0 ? 1 : 0);

                for (int u = 0; u < cs; u++) {
                    for (int v = 0; v < cs; v++) {
                        if (mask[u][v] == null || used[u][v]) continue;
                        FaceCell key = mask[u][v];

                        // Expand width (u direction)
                        int w = 1;
                        while (u + w < cs && key.equals(mask[u + w][v]) && !used[u + w][v]) w++;

                        // Expand height (v direction)
                        int h = 1;
                        outer:
                        while (v + h < cs) {
                            for (int k = u; k < u + w; k++) {
                                if (!key.equals(mask[k][v + h]) || used[k][v + h]) break outer;
                            }
                            h++;
                        }

                        // Mark used
                        for (int k = u; k < u + w; k++)
                            for (int l = v; l < v + h; l++)
                                used[k][l] = true;

                        // Emit merged quad
                        emitQuad(out, dAxis, uAxis, vAxis, nSign, faceDepth,
                                start[uAxis] + u, start[vAxis] + v, w, h,
                                key.r(), key.g(), key.b(), key.texture());
                    }
                }
            }
        }
    }

    /**
     * Emits two triangles for a merged face rectangle.
     *
     * @param faceDepth  world coordinate along dAxis where the face sits
     * @param fu         world coordinate along uAxis (start of rectangle)
     * @param fv         world coordinate along vAxis (start of rectangle)
     * @param w          width in uAxis units
     * @param h          height in vAxis units
     */
    private static void emitQuad(List<DebugVertex> out,
                                  int dAxis, int uAxis, int vAxis, int nSign,
                                  int faceDepth, int fu, int fv, int w, int h,
                                  float r, float g, float b, ResourceId tex) {
        float[] c0 = coord(dAxis, uAxis, vAxis, faceDepth, fu,     fv);
        float[] c1 = coord(dAxis, uAxis, vAxis, faceDepth, fu + w, fv);
        float[] c2 = coord(dAxis, uAxis, vAxis, faceDepth, fu + w, fv + h);
        float[] c3 = coord(dAxis, uAxis, vAxis, faceDepth, fu,     fv + h);

        out.add(dv(c0, r, g, b, 0, 0, tex));
        out.add(dv(c1, r, g, b, 1, 0, tex));
        out.add(dv(c2, r, g, b, 1, 1, tex));
        out.add(dv(c0, r, g, b, 0, 0, tex));
        out.add(dv(c2, r, g, b, 1, 1, tex));
        out.add(dv(c3, r, g, b, 0, 1, tex));
    }

    private static float[] coord(int dAxis, int uAxis, int vAxis, float d, float u, float v) {
        float[] c = new float[3];
        c[dAxis] = d;
        c[uAxis] = u;
        c[vAxis] = v;
        return c;
    }

    private static DebugVertex dv(float[] c, float r, float g, float b, float u, float v, ResourceId tex) {
        return new DebugVertex(c[0], c[1], c[2], r, g, b, u, v, tex);
    }

    /** Mergeability + rendering key for a single face cell. */
    private record FaceCell(float r, float g, float b, ResourceId texture) {}

    // -------------------------------------------------------------------------
    // Helpers (unchanged)
    // -------------------------------------------------------------------------

    private static boolean isInChunk(BlockPos pos, int sx, int sy, int sz) {
        return pos.x() >= sx && pos.x() < sx + CHUNK_SIZE
            && pos.y() >= sy && pos.y() < sy + CHUNK_SIZE
            && pos.z() >= sz && pos.z() < sz + CHUNK_SIZE;
    }

    private RenderModel renderModelFor(BlockState state) {
        if (blockStateRegistry != null) {
            var stateId = blockStateRegistry.id(state);
            if (stateId.isPresent()) {
                RenderModel rm = renderModelsByStateId.get(stateId.getAsInt());
                if (rm != null) return rm;
            }
        }
        return resolveRenderModel(state);
    }

    private RenderModel resolveRenderModel(BlockState state) {
        BlockTextures defaultTextures = texturesByBlock.get(state.id());
        BlockModel model = modelsByBlock.get(state.id());
        if (model == null) model = BlockModel.CUBE_ALL;
        for (BlockModelVariant variant : variantsByBlock.getOrDefault(state.id(), List.of())) {
            if (variant.matches(state)) {
                return new RenderModel(variant.model(),
                        variant.textures() != null ? variant.textures() : defaultTextures);
            }
        }
        return new RenderModel(model, defaultTextures);
    }

    private record RenderModel(BlockModel model, BlockTextures textures) {}

    private static void addModelMesh(List<DebugVertex> vertices, BlockPos pos, ContentModelMesh mesh,
                                      float r, float g, float b, BlockTextures textures) {
        for (ContentModelVertex vertex : mesh.vertices()) {
            ResourceId tex = modelTexture(textures, vertex.textureSlot());
            vertices.add(new DebugVertex(pos.x() + vertex.x(), pos.y() + vertex.y(), pos.z() + vertex.z(),
                    r, g, b, vertex.u(), vertex.v(), tex));
        }
    }

    private static ResourceId modelTexture(BlockTextures textures) {
        return textures == null ? null : textures.textureFor(BlockTextures.Face.UP);
    }

    private static ResourceId modelTexture(BlockTextures textures, String slot) {
        if (textures == null) return null;
        if (slot == null || slot.isBlank()) return modelTexture(textures);
        return switch (slot) {
            case "all"    -> textures.all() != null ? textures.all() : modelTexture(textures);
            case "top"    -> textures.textureFor(BlockTextures.Face.UP);
            case "bottom" -> textures.textureFor(BlockTextures.Face.DOWN);
            case "side"   -> textures.textureFor(BlockTextures.Face.NORTH);
            default       -> modelTexture(textures);
        };
    }

    private static float colorChannel(BlockState state, int salt) {
        int mixed = Math.floorMod(state.id().toString().hashCode() + salt, 100);
        return 0.35f + (mixed / 100.0f) * 0.6f;
    }
}
