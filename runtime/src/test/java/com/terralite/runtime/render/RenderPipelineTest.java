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
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
