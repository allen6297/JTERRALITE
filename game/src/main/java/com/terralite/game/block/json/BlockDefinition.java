package com.terralite.game.block.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.block.Block;

import java.util.List;

public record BlockDefinition(
    @JsonProperty("display_name")
    String displayName,
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
    String soundType,
    List<String> categories
) {
    public Block toBlock() {
        return Block.builder()
            .displayName(displayName != null ? displayName : "")
            .hardness(hardness)
            .resistance(resistance)
            .solid(solid)
            .transparent(transparent)
            .requiresTool(requiresTool)
            .material(defaultString(material, "stone"))
            .soundType(defaultString(soundType, "stone"))
            .categories(parseCategories(categories))
            .build();
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static List<ResourceId> parseCategories(List<String> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
            .map(ResourceId::id)
            .toList();
    }
}
