package com.terralite.game.block;

import com.terralite.core.registry.ResourceId;

import java.util.List;
import java.util.Objects;

public record BlockProperties(
    float hardness,
    float resistance,
    boolean solid,
    boolean transparent,
    boolean requiresTool,
    String material,
    String soundType,
    List<ResourceId> categories
) {
    public BlockProperties {
        if (hardness < 0.0f) {
            throw new IllegalArgumentException("Block hardness cannot be negative");
        }

        if (resistance < 0.0f) {
            throw new IllegalArgumentException("Block resistance cannot be negative");
        }

        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(soundType, "soundType");
        categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
    }
}
