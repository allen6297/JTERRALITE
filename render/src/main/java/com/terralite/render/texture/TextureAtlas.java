package com.terralite.render.texture;

import com.terralite.core.registry.ResourceId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TextureAtlas(int width, int height, int[] argbPixels, Map<ResourceId, TextureRegion> regions) {
    public static final ResourceId FONT_ASCII = ResourceId.id("terralite:ui/font_ascii");

    public TextureAtlas {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture atlas dimensions must be positive");
        }
        argbPixels = Objects.requireNonNull(argbPixels, "argbPixels").clone();
        if (argbPixels.length != width * height) {
            throw new IllegalArgumentException("Texture atlas pixel count must match dimensions");
        }
        regions = Map.copyOf(Objects.requireNonNull(regions, "regions"));
    }

    public Optional<TextureRegion> region(ResourceId id) {
        return Optional.ofNullable(regions.get(Objects.requireNonNull(id, "id")));
    }
}
