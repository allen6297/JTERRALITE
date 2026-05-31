package com.terralite.runtime.render;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.world.World;
import com.terralite.render.ClearColor;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderState;
import com.terralite.render.RenderStats;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;
import com.terralite.render.backend.RecordingRenderBackend;
import com.terralite.render.texture.TextureAtlas;
import com.terralite.render.texture.TextureRegion;
import org.junit.jupiter.api.Test;

import com.terralite.content.assets.model.ContentModelMesh;
import com.terralite.content.assets.model.ContentModelVertex;
import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.game.block.Block;
import com.terralite.game.block.BlockModel;
import com.terralite.game.block.BlockTextures;
import com.terralite.game.registry.TerraliteRegistries;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RenderPipelineTest {
    @Test
    void pipelineBuildsSceneAndRendersFrame() {
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        Camera camera = new Camera(new Transform(1.0, 2.0, 3.0), 70.0, 0.01, 1_000.0);
        RecordingRenderBackend backend = new RecordingRenderBackend();
        Renderer renderer = new Renderer(backend);
        RenderPipeline pipeline = new RenderPipeline(
                world,
                camera,
                renderer,
                new Viewport(1280, 720),
                new ClearColor(0.1f, 0.2f, 0.3f, 1.0f)
        );

        renderer.start();
        RenderStats stats = pipeline.renderFrame();

        assertEquals(RenderState.RUNNING, renderer.state());
        assertEquals(1, stats.frameIndex());
        assertEquals(new Viewport(1280, 720), stats.viewport());
        assertEquals(new ClearColor(0.1f, 0.2f, 0.3f, 1.0f), backend.frames().get(0).clearColor());
        assertEquals(List.of(new RenderChunk(0, 0, 0)), backend.frames().get(0).scene().chunks());
    }

    @Test
    void pipelineAllowsViewportAndClearColorUpdates() {
        World world = new World();
        Camera camera = new Camera();
        RecordingRenderBackend backend = new RecordingRenderBackend();
        Renderer renderer = new Renderer(backend);
        RenderPipeline pipeline = new RenderPipeline(
                world,
                camera,
                renderer,
                new Viewport(800, 600),
                ClearColor.BLACK
        );

        pipeline.setViewport(new Viewport(1920, 1080));
        pipeline.setClearColor(new ClearColor(0.2f, 0.3f, 0.4f, 1.0f));

        assertSame(world, pipeline.world());
        assertSame(camera, pipeline.camera());
        assertSame(renderer, pipeline.renderer());
        assertEquals(new Viewport(1920, 1080), pipeline.viewport());
        assertEquals(new ClearColor(0.2f, 0.3f, 0.4f, 1.0f), pipeline.clearColor());
    }

    @Test
    void pipelineRemapsTextureUvsThroughAtlasBeforeRendering() {
        ResourceId blockId = ResourceId.id("terralite:textured_block");
        ResourceId modelId = ResourceId.id("terralite:block/textured_triangle");
        ResourceId textureId = ResourceId.id("terralite:block/texture");
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.BLOCKS).register(blockId, Block.builder()
                .model(new BlockModel(modelId))
                .textures(BlockTextures.all(textureId))
                .build());
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.setBlock(BlockPos.of(0, 0, 0), new BlockState(blockId));
        Camera camera = new Camera();
        RecordingRenderBackend backend = new RecordingRenderBackend();
        Renderer renderer = new Renderer(backend);
        TextureAtlas atlas = new TextureAtlas(4, 2, new int[8], Map.of(
                textureId, new TextureRegion(0.5f, 0.0f, 1.0f, 1.0f)
        ));
        ContentModelMesh modelMesh = new ContentModelMesh(List.of(
                new ContentModelVertex(0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
                new ContentModelVertex(1.0f, 0.0f, 0.0f, 1.0f, 0.0f),
                new ContentModelVertex(0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
        ));
        RenderPipeline pipeline = new RenderPipeline(
                world,
                camera,
                renderer,
                new Viewport(128, 128),
                ClearColor.BLACK,
                registries.freeze(),
                Map.of(modelId, modelMesh),
                atlas
        );

        renderer.start();
        pipeline.renderFrame();

        var vertices = backend.frames().get(0).scene().chunkMeshes().get(0).mesh().vertices();
        assertEquals(0.5f, vertices.get(0).u());
        assertEquals(1.0f, vertices.get(1).u());
        assertEquals(1.0f, vertices.get(2).v());
        assertEquals(atlas, backend.frames().get(0).textureAtlas());
    }
}
