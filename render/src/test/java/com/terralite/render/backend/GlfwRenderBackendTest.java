package com.terralite.render.backend;

import com.terralite.render.RenderFrame;
import com.terralite.render.RenderStats;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;
import com.terralite.render.window.RenderWindow;
import com.terralite.render.window.WindowConfig;
import com.terralite.render.window.WindowState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GlfwRenderBackendTest {
    @Test
    void validatesWindowConfig() {
        assertThrows(IllegalArgumentException.class, () -> new WindowConfig("", 800, 600));
        assertThrows(IllegalArgumentException.class, () -> new WindowConfig("TERRALITE", 0, 600));
        assertThrows(IllegalArgumentException.class, () -> new WindowConfig("TERRALITE", 800, 0));
    }

    @Test
    void backendDelegatesLifecycleAndReportsWindowViewport() {
        RecordingWindow window = new RecordingWindow(new WindowConfig("TERRALITE", 1024, 768, false));
        try (Renderer renderer = new Renderer(new GlfwRenderBackend(window))) {
            renderer.start();
            RenderStats stats = renderer.render(RenderFrame.of(800, 600));
            renderer.stop();

            assertEquals(List.of("create", "show", "poll", "viewport", "destroy"), window.events());
            assertEquals(new Viewport(1024, 768), stats.viewport());
            assertEquals(1, stats.frameIndex());
        }

        assertEquals(WindowState.CLOSED, window.state());
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
