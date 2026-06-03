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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ChunkMeshBuilder {
    public static final int CHUNK_SIZE = 16;
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

        List<DebugVertex> vertices = new ArrayList<>();
        int minX = chunk.x() * CHUNK_SIZE;
        int minY = chunk.y() * CHUNK_SIZE;
        int minZ = chunk.z() * CHUNK_SIZE;
        int maxX = minX + CHUNK_SIZE;
        int maxY = minY + CHUNK_SIZE;
        int maxZ = minZ + CHUNK_SIZE;

        world.blocks().positions().stream()
                .filter(pos -> blockIntersectsChunk(world.getBlock(pos), pos, minX, minY, minZ, maxX, maxY, maxZ))
                .sorted(Comparator
                        .comparingInt(BlockPos::x)
                        .thenComparingInt(BlockPos::y)
                        .thenComparingInt(BlockPos::z))
                .forEach(pos -> addVisibleBlockFaces(vertices, world, pos));

        if (vertices.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RenderChunkMesh(chunk, new DebugMesh(vertices)));
    }

    private void addVisibleBlockFaces(List<DebugVertex> vertices, World world, BlockPos pos) {
        BlockState state = world.getBlock(pos);
        if (state.isAir()) {
            return;
        }

        float red = colorChannel(state, 0);
        float green = colorChannel(state, 37);
        float blue = colorChannel(state, 73);
        RenderModel renderModel = renderModelFor(state);
        BlockTextures textures = renderModel.textures();
        ContentModelMesh modelMesh = modelMeshes.get(renderModel.model().id());
        if (modelMesh != null) {
            addModelMesh(vertices, pos, modelMesh, red, green, blue, textures);
            return;
        }

        for (Face face : Face.values()) {
            if (world.getBlock(face.neighbor(pos)).isAir()) {
                addFace(vertices, pos, face, red, green, blue, textureFor(textures, face));
            }
        }
    }

    private RenderModel renderModelFor(BlockState state) {
        if (blockStateRegistry != null) {
            var stateId = blockStateRegistry.id(state);
            if (stateId.isPresent()) {
                RenderModel renderModel = renderModelsByStateId.get(stateId.getAsInt());
                if (renderModel != null) {
                    return renderModel;
                }
            }
        }
        return resolveRenderModel(state);
    }

    private RenderModel resolveRenderModel(BlockState state) {
        BlockTextures defaultTextures = texturesByBlock.get(state.id());
        BlockModel model = modelsByBlock.get(state.id());
        if (model == null) {
            model = BlockModel.CUBE_ALL;
        }
        for (BlockModelVariant variant : variantsByBlock.getOrDefault(state.id(), List.of())) {
            if (variant.matches(state)) {
                return new RenderModel(
                        variant.model(),
                        variant.textures() != null ? variant.textures() : defaultTextures
                );
            }
        }
        return new RenderModel(model, defaultTextures);
    }

    private boolean blockIntersectsChunk(
            BlockState state,
            BlockPos pos,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        if (state.isAir()) {
            return false;
        }
        RenderModel renderModel = renderModelFor(state);
        ContentModelMesh modelMesh = modelMeshes.get(renderModel.model().id());
        float blockMinX = pos.x() + (modelMesh != null ? modelMesh.minX() : 0.0f);
        float blockMinY = pos.y() + (modelMesh != null ? modelMesh.minY() : 0.0f);
        float blockMinZ = pos.z() + (modelMesh != null ? modelMesh.minZ() : 0.0f);
        float blockMaxX = pos.x() + (modelMesh != null ? modelMesh.maxX() : 1.0f);
        float blockMaxY = pos.y() + (modelMesh != null ? modelMesh.maxY() : 1.0f);
        float blockMaxZ = pos.z() + (modelMesh != null ? modelMesh.maxZ() : 1.0f);

        return blockMaxX > minX && blockMinX < maxX
                && blockMaxY > minY && blockMinY < maxY
                && blockMaxZ > minZ && blockMinZ < maxZ;
    }

    private record RenderModel(BlockModel model, BlockTextures textures) {
    }

    private static void addModelMesh(
            List<DebugVertex> vertices,
            BlockPos pos,
            ContentModelMesh mesh,
            float red,
            float green,
            float blue,
            BlockTextures textures
    ) {
        for (ContentModelVertex vertex : mesh.vertices()) {
            ResourceId vertexTexture = modelTexture(textures, vertex.textureSlot());
            vertices.add(new DebugVertex(
                    pos.x() + vertex.x(),
                    pos.y() + vertex.y(),
                    pos.z() + vertex.z(),
                    red,
                    green,
                    blue,
                    vertex.u(),
                    vertex.v(),
                    vertexTexture
            ));
        }
    }

    private static void addFace(
            List<DebugVertex> vertices,
            BlockPos pos,
            Face face,
            float red,
            float green,
            float blue,
            ResourceId texture
    ) {
        float x = pos.x();
        float y = pos.y();
        float z = pos.z();
        for (int index : face.indices) {
            float[] corner = face.corners[index];
            float[] uv = face.uvs[index];
            vertices.add(new DebugVertex(
                    x + corner[0],
                    y + corner[1],
                    z + corner[2],
                    red,
                    green,
                    blue,
                    uv[0],
                    uv[1],
                    texture
            ));
        }
    }

    private static ResourceId textureFor(BlockTextures textures, Face face) {
        if (textures == null) {
            return null;
        }
        return textures.textureFor(switch (face) {
            case EAST -> BlockTextures.Face.EAST;
            case WEST -> BlockTextures.Face.WEST;
            case UP -> BlockTextures.Face.UP;
            case DOWN -> BlockTextures.Face.DOWN;
            case SOUTH -> BlockTextures.Face.SOUTH;
            case NORTH -> BlockTextures.Face.NORTH;
        });
    }

    private static ResourceId modelTexture(BlockTextures textures) {
        return textures == null ? null : textures.textureFor(BlockTextures.Face.UP);
    }

    private static ResourceId modelTexture(BlockTextures textures, String textureSlot) {
        if (textures == null) {
            return null;
        }
        if (textureSlot == null || textureSlot.isBlank()) {
            return modelTexture(textures);
        }
        return switch (textureSlot) {
            case "all" -> textures.all() != null ? textures.all() : modelTexture(textures);
            case "top" -> textures.textureFor(BlockTextures.Face.UP);
            case "bottom" -> textures.textureFor(BlockTextures.Face.DOWN);
            case "side" -> textures.textureFor(BlockTextures.Face.NORTH);
            default -> modelTexture(textures);
        };
    }

    private static float colorChannel(BlockState state, int salt) {
        int mixed = Math.floorMod(state.id().toString().hashCode() + salt, 100);
        return 0.35f + (mixed / 100.0f) * 0.6f;
    }

    private enum Face {
        EAST(1, 0, 0, new float[][] {
                {1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}
        }),
        WEST(-1, 0, 0, new float[][] {
                {0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}
        }),
        UP(0, 1, 0, new float[][] {
                {0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}
        }),
        DOWN(0, -1, 0, new float[][] {
                {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}
        }),
        SOUTH(0, 0, 1, new float[][] {
                {1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}
        }),
        NORTH(0, 0, -1, new float[][] {
                {0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}
        });

        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;
        private final float[][] corners;
        private final float[][] uvs = {
                {0, 0}, {1, 0}, {1, 1}, {0, 1}
        };
        private final int[] indices = {0, 1, 2, 0, 2, 3};

        Face(int offsetX, int offsetY, int offsetZ, float[][] corners) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.corners = corners;
        }

        private BlockPos neighbor(BlockPos pos) {
            return BlockPos.of(pos.x() + offsetX, pos.y() + offsetY, pos.z() + offsetZ);
        }
    }
}
