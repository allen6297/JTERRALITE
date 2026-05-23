package com.terralite.game.block;

import java.util.Objects;

public record BlockProperties(
    float hardness,
    float resistance,
    boolean solid,
    boolean transparent,
    boolean requiresTool,
    String material,
    String soundType
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
    }
}
