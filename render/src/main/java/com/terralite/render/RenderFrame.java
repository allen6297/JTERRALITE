package com.terralite.render;

import com.terralite.render.texture.TextureAtlas;

import java.util.Objects;

public record RenderFrame(Viewport viewport, ClearColor clearColor, RenderScene scene, TextureAtlas textureAtlas) {
    public RenderFrame {
        Objects.requireNonNull(viewport, "viewport");
        Objects.requireNonNull(clearColor, "clearColor");
        Objects.requireNonNull(scene, "scene");
    }

    public RenderFrame(Viewport viewport, ClearColor clearColor, RenderScene scene) {
        this(viewport, clearColor, scene, null);
    }

    public RenderFrame(Viewport viewport, ClearColor clearColor) {
        this(viewport, clearColor, RenderScene.empty());
    }

    public static RenderFrame of(int width, int height) {
        return new RenderFrame(new Viewport(width, height), ClearColor.BLACK);
    }
}
