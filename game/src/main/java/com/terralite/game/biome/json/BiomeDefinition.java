package com.terralite.game.biome.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.game.biome.Biome;

public record BiomeDefinition(
        String name,
        int priority,
        double rarity,
        Climate climate,
        Terrain terrain,
        Surface surface
) {
    record Climate(MinMax temperature, MinMax humidity) {}
    record MinMax(double min, double max) {}
    record Terrain(
            @JsonProperty("base_height") int baseHeight,
            @JsonProperty("height_variation") int heightVariation
    ) {}
    record Surface(
            String top,
            String middle,
            @JsonProperty("middle_depth") int middleDepth,
            String base
    ) {}

    public Biome toBiome() {
        Biome.Builder builder = Biome.builder()
                .name(name != null ? name : "")
                .priority(priority)
                .rarity(rarity > 0 ? rarity : 1.0);

        if (climate != null) {
            if (climate.temperature() != null) {
                builder.temperature(climate.temperature().min(), climate.temperature().max());
            }
            if (climate.humidity() != null) {
                builder.humidity(climate.humidity().min(), climate.humidity().max());
            }
        }

        if (terrain != null) {
            builder.baseHeight(terrain.baseHeight());
            builder.heightVariation(terrain.heightVariation());
        }

        if (surface != null) {
            builder.surfaceTop(surface.top());
            builder.surfaceMiddle(surface.middle());
            builder.surfaceMiddleDepth(surface.middleDepth());
            builder.surfaceBase(surface.base());
        }

        return builder.build();
    }
}
