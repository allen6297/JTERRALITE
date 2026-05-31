package com.terralite.runtime.render;

import com.terralite.content.assets.model.ContentModelMesh;
import com.terralite.content.assets.model.ContentModelVertex;
import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.world.World;
import com.terralite.game.block.Block;
import com.terralite.game.block.BlockModel;
import com.terralite.game.block.BlockTextures;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.render.RenderChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkMeshBuilderTest {
    private final ChunkMeshBuilder builder = new ChunkMeshBuilder();

    @Test
    void singleSolidBlockCreatesSixVisibleFaces() {
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.setBlock(BlockPos.of(0, 0, 0), BlockState.of("terralite:stone"));

        var mesh = builder.build(world, new RenderChunk(0, 0, 0)).orElseThrow();

        assertEquals(36, mesh.mesh().vertices().size());
    }

    @Test
    void adjacentBlocksCullSharedInternalFaces() {
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.setBlock(BlockPos.of(0, 0, 0), BlockState.of("terralite:stone"));
        world.setBlock(BlockPos.of(1, 0, 0), BlockState.of("terralite:stone"));

        var mesh = builder.build(world, new RenderChunk(0, 0, 0)).orElseThrow();

        assertEquals(60, mesh.mesh().vertices().size());
    }

    @Test
    void emptyChunkDoesNotCreateMesh() {
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));

        assertTrue(builder.build(world, new RenderChunk(0, 0, 0)).isEmpty());
    }

    @Test
    void meshCarriesFaceUvsAndTextureIdsFromGameData() {
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:stone"), Block.builder()
                        .textures(BlockTextures.all(ResourceId.id("terralite:block/stone")))
                        .build());
        ChunkMeshBuilder texturedBuilder = new ChunkMeshBuilder(registries.freeze());
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.setBlock(BlockPos.of(0, 0, 0), BlockState.of("terralite:stone"));

        var mesh = texturedBuilder.build(world, new RenderChunk(0, 0, 0)).orElseThrow();

        assertEquals(ResourceId.id("terralite:block/stone"), mesh.mesh().vertices().get(0).texture());
        assertEquals(0.0f, mesh.mesh().vertices().get(0).u());
        assertEquals(0.0f, mesh.mesh().vertices().get(0).v());
        assertEquals(1.0f, mesh.mesh().vertices().get(2).u());
        assertEquals(1.0f, mesh.mesh().vertices().get(2).v());
    }

    @Test
    void usesParsedModelMeshWhenBlockModelIsLoaded() {
        ResourceId modelId = ResourceId.id("terralite:block/custom_triangle");
        ResourceId textureId = ResourceId.id("terralite:block/custom_texture");
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:custom_block"), Block.builder()
                        .model(new BlockModel(modelId))
                        .textures(BlockTextures.all(textureId))
                        .build());
        ContentModelMesh modelMesh = new ContentModelMesh(List.of(
                new ContentModelVertex(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
                new ContentModelVertex(1.0f, 0.0f, 0.0f, 1.0f, 0.0f),
                new ContentModelVertex(0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
        ));
        ChunkMeshBuilder modelBuilder = new ChunkMeshBuilder(registries.freeze(), Map.of(modelId, modelMesh));
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.setBlock(BlockPos.of(3, 4, 5), BlockState.of("terralite:custom_block"));

        var mesh = modelBuilder.build(world, new RenderChunk(0, 0, 0)).orElseThrow();

        assertEquals(3, mesh.mesh().vertices().size());
        assertEquals(3.0f, mesh.mesh().vertices().get(0).x());
        assertEquals(4.0f, mesh.mesh().vertices().get(0).y());
        assertEquals(5.0f, mesh.mesh().vertices().get(0).z());
        assertEquals(4.0f, mesh.mesh().vertices().get(1).x());
        assertEquals(textureId, mesh.mesh().vertices().get(0).texture());
    }
}
