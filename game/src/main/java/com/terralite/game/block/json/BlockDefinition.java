package com.terralite.game.block.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.game.block.Block;

public record BlockDefinition(
    @JsonProperty(defaultValue = "1.0")
    float hardness,
    @JsonProperty(defaultValue = "1.0")
    float resistance,
    @JsonProperty(defaultValue = "true")
    boolean solid,
    boolean transparent,
    @JsonProperty("requires_tool")
    boolean requiresTool,
    @JsonProperty(defaultValue = "stone")
    String material,
    @JsonProperty("sound_type")
    String soundType
) {
    public Block toBlock() {
        return Block.builder()
            .hardness(hardness)
            .resistance(resistance)
            .solid(solid)
            .transparent(transparent)
            .requiresTool(requiresTool)
            .material(defaultString(material, "stone"))
            .soundType(defaultString(soundType, "stone"))
            .build();
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
