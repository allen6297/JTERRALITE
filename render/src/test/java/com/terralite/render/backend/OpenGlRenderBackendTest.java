package com.terralite.render.backend;

import com.terralite.render.ClearColor;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderScene;
import com.terralite.render.RenderStats;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.opengl.OpenGlCommands;
import com.terralite.render.window.RenderWindow;
import com.terralite.render.window.WindowConfig;
import com.terralite.render.window.WindowState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenGlRenderBackendTest {
    @Test
    void backendClearsFrameAndDrawsChunkMarkers() {
        RecordingWindow window = new RecordingWindow(WindowConfig.openGl("TERRALITE", 640, 480));
        RecordingOpenGlCommands commands = new RecordingOpenGlCommands();
        Renderer renderer = new Renderer(new OpenGlRenderBackend(window, commands));

        renderer.start();
        RenderScene scene = RenderScene.builder()
                .addChunk(new RenderChunk(0, 0, 0))
                .addChunk(new RenderChunk(1, 0, 0))
                .build();
        RenderStats stats = renderer.render(new RenderFrame(
                new Viewport(800, 600),
                new ClearColor(0.25f, 0.5f, 0.75f, 1.0f),
                scene
        ));
        renderer.stop();

        assertEquals(List.of("create", "context", "show", "poll", "viewport", "swap", "destroy"), window.events());
        assertEquals(List.of(
                "capabilities",
                "viewport:640x480",
                "clear:0.25,0.5,0.75,1.0",
                "createMesh:6",
                "drawMesh:1",
                "createMesh:6",
                "drawMesh:2",
                "destroyMesh:1",
                "destroyMesh:2"
        ), commands.events());
        assertEquals(new Viewport(640, 480), stats.viewport());
        assertEquals(1, stats.frameIndex());
    }

    @Test
    void backendReusesChunkMarkerMeshesAcrossFramesAndDestroysRemovedChunks() {
        RecordingWindow window = new RecordingWindow(WindowConfig.openGl("TERRALITE", 640, 480));
        RecordingOpenGlCommands commands = new RecordingOpenGlCommands();
        Renderer renderer = new Renderer(new OpenGlRenderBackend(window, commands));
        RenderScene firstScene = RenderScene.builder()
                .addChunk(new RenderChunk(0, 0, 0))
                .addChunk(new RenderChunk(1, 0, 0))
                .build();
        RenderScene secondScene = RenderScene.builder()
                .addChunk(new RenderChunk(1, 0, 0))
                .build();

        renderer.start();
        renderer.render(new RenderFrame(new Viewport(800, 600), ClearColor.BLACK, firstScene));
        renderer.render(new RenderFrame(new Viewport(800, 600), ClearColor.BLACK, secondScene));
        renderer.stop();

        assertEquals(List.of(
                "capabilities",
                "viewport:640x480",
                "clear:0.0,0.0,0.0,1.0",
                "createMesh:6",
                "drawMesh:1",
                "createMesh:6",
                "drawMesh:2",
                "viewport:640x480",
                "clear:0.0,0.0,0.0,1.0",
                "destroyMesh:1",
                "drawMesh:2",
                "destroyMesh:2"
        ), commands.events());
    }

    private static final class RecordingOpenGlCommands implements OpenGlCommands {
        private final List<String> events = new ArrayList<>();
        private int nextMeshHandle = 1;

        @Override
        public void createCapabilities() {
            events.add("capabilities");
        }

        @Override
        public int createMesh(DebugMesh mesh) {
            events.add("createMesh:" + mesh.vertices().size());
            return nextMeshHandle++;
        }

        @Override
        public void viewport(Viewport viewport) {
            events.add("viewport:" + viewport.width() + "x" + viewport.height());
        }

        @Override
        public void clear(ClearColor clearColor) {
            events.add("clear:" + clearColor.red() + "," + clearColor.green() + ","
                    + clearColor.blue() + "," + clearColor.alpha());
        }

        @Override
        public void drawMesh(int meshHandle, float[] mvp) {
            events.add("drawMesh:" + meshHandle);
        }

        @Override
        public void destroyMesh(int meshHandle) {
            events.add("destroyMesh:" + meshHandle);
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }

    private static final class RecordingWindow implements RenderWindow {
        private final WindowConfig config;
        private final List<String> events = new ArrayList<>();
        private WindowState state = WindowState.CREATED;

        private RecordingWindow(WindowConfig config) {
            this.config = config;
        }

        @Override
        public WindowConfig config() {
            return config;
        }

        @Override
        public WindowState state() {
            return state;
        }

        @Override
        public void create() {
            events.add("create");
            state = WindowState.OPEN;
        }

        @Override
        public void show() {
            events.add("show");
        }

        @Override
        public void makeContextCurrent() {
            events.add("context");
        }

        @Override
        public void swapBuffers() {
            events.add("swap");
        }

        @Override
        public void pollEvents() {
            events.add("poll");
        }

        @Override
        public boolean shouldClose() {
            return false;
        }

        @Override
        public Viewport viewport() {
            events.add("viewport");
            return new Viewport(config.width(), config.height());
        }

        @Override
        public void destroy() {
            events.add("destroy");
            state = WindowState.CLOSED;
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
