package com.terralite.render;

import com.terralite.render.backend.RecordingRenderBackend;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RendererTest {
    @Test
    void rendererInitializesStartsRendersAndStopsBackend() {
        RecordingRenderBackend backend = new RecordingRenderBackend();
        Renderer renderer = new Renderer(backend);

        assertEquals(RenderState.CREATED, renderer.state());
        assertEquals(List.of("initialize"), backend.lifecycleEvents());

        renderer.start();
        RenderStats stats = renderer.render(RenderFrame.of(1280, 720));
        renderer.stop();

        assertEquals(RenderState.STOPPED, renderer.state());
        assertEquals(1, stats.frameIndex());
        assertEquals(new Viewport(1280, 720), stats.viewport());
        assertEquals(List.of("initialize", "start", "stop"), backend.lifecycleEvents());
        assertEquals(1, backend.frames().size());
    }

    @Test
    void renderFrameCanSubmitSceneData() {
        RecordingRenderBackend backend = new RecordingRenderBackend();
        Renderer renderer = new Renderer(backend);
        RenderScene scene = RenderScene.builder()
            .camera(new RenderCamera(1.0, 2.0, 3.0, 75.0, 0.1, 500.0))
            .addChunk(new RenderChunk(0, 0, 0))
            .addObject(RenderObject.of("terralite:test_object", 4.0, 5.0, 6.0))
            .build();

        renderer.start();
        renderer.render(new RenderFrame(new Viewport(800, 600), ClearColor.BLACK, scene));

        RenderFrame submitted = backend.frames().get(0);
        assertEquals(scene, submitted.scene());
        assertEquals(new RenderCamera(1.0, 2.0, 3.0, 75.0, 0.1, 500.0), submitted.scene().camera());
        assertEquals(List.of(new RenderChunk(0, 0, 0)), submitted.scene().chunks());
        assertEquals(List.of(RenderObject.of("terralite:test_object", 4.0, 5.0, 6.0)), submitted.scene().objects());
    }

    @Test
    void rendererRejectsRenderBeforeStart() {
        Renderer renderer = new Renderer(new RecordingRenderBackend());

        assertThrows(IllegalStateException.class, () -> renderer.render(RenderFrame.of(800, 600)));
    }

    @Test
    void rendererCannotRestartAfterStop() {
        Renderer renderer = new Renderer(new RecordingRenderBackend());

        renderer.start();
        renderer.stop();

        assertThrows(IllegalStateException.class, renderer::start);
    }

    @Test
    void viewportCalculatesAspectRatio() {
        assertEquals(16.0 / 9.0, new Viewport(1920, 1080).aspectRatio());
    }

    @Test
    void frameValuesRejectInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new Viewport(0, 720));
        assertThrows(IllegalArgumentException.class, () -> new ClearColor(-0.1f, 0.0f, 0.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> new RenderCamera(0.0, 0.0, 0.0, 180.0, 0.1, 1_000.0));
    }
}
