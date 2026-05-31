package com.terralite.game.block.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.block.Block;
import com.terralite.game.block.BlockModel;
import com.terralite.game.block.BlockTextures;

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
    String model,
    TextureDefinition textures,
    List<String> categories
) {
    public Block toBlock() {
        Block.Builder builder = Block.builder()
                .displayName(displayName != null ? displayName : "")
                .hardness(hardness)
                .resistance(resistance)
                .solid(solid)
                .transparent(transparent)
                .requiresTool(requiresTool)
                .material(defaultString(material, "stone"))
                .soundType(defaultString(soundType, "stone"))
                .model(parseModel(model))
                .categories(parseCategories(categories));
        BlockTextures parsedTextures = parseTextures(textures);
        if (parsedTextures != null) {
            builder.textures(parsedTextures);
        }
        return builder.build();
    }

    public record TextureDefinition(String all, String top, String bottom, String side) {
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

    private static BlockTextures parseTextures(TextureDefinition textures) {
        if (textures == null) {
            return null;
        }
        return new BlockTextures(
                parseNullableId(textures.all()),
                parseNullableId(textures.top()),
                parseNullableId(textures.bottom()),
                parseNullableId(textures.side())
        );
    }

    private static ResourceId parseNullableId(String value) {
        return value == null || value.isBlank() ? null : ResourceId.id(value);
    }

    private static BlockModel parseModel(String model) {
        return model == null || model.isBlank() ? BlockModel.CUBE_ALL : BlockModel.of(model);
    }
}
