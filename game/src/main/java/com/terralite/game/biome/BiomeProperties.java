package com.terralite.game.biome;

import java.util.Objects;

public record BiomeProperties(
        String name,
        int priority,
        double rarity,
        double temperatureMin,
        double temperatureMax,
        double humidityMin,
        double humidityMax,
        int baseHeight,
        int heightVariation,
        String surfaceTop,
        String surfaceMiddle,
        int surfaceMiddleDepth,
        String surfaceBase
) {
    public BiomeProperties {
        Objects.requireNonNull(name, "name");
        if (rarity < 0.0 || rarity > 1.0) {
            throw new IllegalArgumentException("Biome rarity must be between 0.0 and 1.0");
        }
        if (temperatureMin > temperatureMax) {
            throw new IllegalArgumentException("Biome temperature min must not exceed max");
        }
        if (humidityMin > humidityMax) {
            throw new IllegalArgumentException("Biome humidity min must not exceed max");
        }
        if (surfaceMiddleDepth < 0) {
            throw new IllegalArgumentException("Biome surface middle depth must not be negative");
        }
    }
}
