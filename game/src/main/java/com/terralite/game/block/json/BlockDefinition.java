package com.terralite.game.block.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.block.Block;
import com.terralite.game.block.BlockModel;
import com.terralite.game.block.BlockModelVariant;
import com.terralite.game.block.BlockStateDefinition;
import com.terralite.game.block.BlockTextures;

import java.util.List;
import java.util.Map;

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
    StateSchemaDefinition state,
    List<StateDefinition> states,
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
                .stateDefinition(parseStateDefinition(state))
                .modelVariants(parseStates(states))
                .categories(parseCategories(categories));
        BlockTextures parsedTextures = parseTextures(textures);
        if (parsedTextures != null) {
            builder.textures(parsedTextures);
        }
        return builder.build();
    }

    public record TextureDefinition(String all, String top, String bottom, String side) {
    }

    public record StateSchemaDefinition(
            Map<String, List<String>> properties,
            @JsonProperty("default")
            Map<String, String> defaultValues
    ) {
    }

    public record StateDefinition(Map<String, String> when, String model, TextureDefinition textures) {
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

    private static List<BlockModelVariant> parseStates(List<StateDefinition> states) {
        if (states == null) {
            return List.of();
        }
        return states.stream()
                .map(state -> new BlockModelVariant(
                        state.when(),
                        parseModel(state.model()),
                        parseTextures(state.textures())
                ))
                .toList();
    }

    private static BlockStateDefinition parseStateDefinition(StateSchemaDefinition state) {
        if (state == null) {
            return BlockStateDefinition.EMPTY;
        }
        return new BlockStateDefinition(
                state.properties() == null ? Map.of() : state.properties(),
                state.defaultValues() == null ? Map.of() : state.defaultValues()
        );
    }

    private static ResourceId parseNullableId(String value) {
        return value == null || value.isBlank() ? null : ResourceId.id(value);
    }

    private static BlockModel parseModel(String model) {
        return model == null || model.isBlank() ? BlockModel.CUBE_ALL : BlockModel.of(model);
    }
}
