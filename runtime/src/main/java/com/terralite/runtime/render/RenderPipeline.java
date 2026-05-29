package com.terralite.runtime.render;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.world.World;
import com.terralite.render.ClearColor;
import com.terralite.render.RenderFrame;
import com.terralite.render.RenderScene;
import com.terralite.render.RenderStats;
import com.terralite.render.Renderer;
import com.terralite.render.Viewport;

import java.util.Objects;

public final class RenderPipeline {
    private final World world;
    private final Camera camera;
    private final Renderer renderer;
    private Viewport viewport;
    private ClearColor clearColor;

    public RenderPipeline(World world, Camera camera, Renderer renderer, Viewport viewport, ClearColor clearColor) {
        this.world = Objects.requireNonNull(world, "world");
        this.camera = Objects.requireNonNull(camera, "camera");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.clearColor = Objects.requireNonNull(clearColor, "clearColor");
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

    public RenderStats renderFrame() {
        RenderScene scene = RenderSceneExtractor.from(world, camera);
        return renderer.render(new RenderFrame(viewport, clearColor, scene));
    }
}
