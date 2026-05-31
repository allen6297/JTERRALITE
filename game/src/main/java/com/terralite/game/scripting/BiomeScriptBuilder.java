package com.terralite.game.scripting;

import com.terralite.core.registry.ResourceId;
import com.terralite.game.biome.Biome;

public final class BiomeScriptBuilder {
    private final ResourceId id;
    private final Biome.Builder builder;

    BiomeScriptBuilder(ResourceId id) {
        this.id = id;
        this.builder = Biome.builder();
    }

    public ResourceId id() {
        return id;
    }

    public BiomeScriptBuilder name(String name) {
        builder.name(name != null ? name : "");
        return this;
    }

    public BiomeScriptBuilder priority(int priority) {
        builder.priority(priority);
        return this;
    }

    public BiomeScriptBuilder rarity(double rarity) {
        builder.rarity(rarity);
        return this;
    }

    public BiomeScriptBuilder temperature(double min, double max) {
        builder.temperature(min, max);
        return this;
    }

    public BiomeScriptBuilder humidity(double min, double max) {
        builder.humidity(min, max);
        return this;
    }

    public BiomeScriptBuilder terrain(int baseHeight, int heightVariation) {
        builder.baseHeight(baseHeight).heightVariation(heightVariation);
        return this;
    }

    public BiomeScriptBuilder surfaceTop(String blockId) {
        builder.surfaceTop(blockId);
        return this;
    }

    public BiomeScriptBuilder surfaceMiddle(String blockId) {
        builder.surfaceMiddle(blockId);
        return this;
    }

    public BiomeScriptBuilder surfaceMiddleDepth(int depth) {
        builder.surfaceMiddleDepth(depth);
        return this;
    }

    public BiomeScriptBuilder surfaceBase(String blockId) {
        builder.surfaceBase(blockId);
        return this;
    }

    Biome build() {
        return builder.build();
    }
}
