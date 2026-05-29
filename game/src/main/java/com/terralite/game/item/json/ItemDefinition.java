package com.terralite.game.item.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.item.Item;

import java.util.List;

public record ItemDefinition(
        @JsonProperty("display_name")
        String displayName,
        @JsonProperty(defaultValue = "1.0")
        float weight,
        @JsonProperty("stack_size")
        Integer stackSize,
        @JsonProperty("places_block")
        String placesBlock,
        List<String> categories
        ) {
    public Item toItem() {
        return Item.builder()
                .displayName(displayName != null ? displayName : "")
                .weight(weight)
                .stackSize(stackSize != null ? stackSize : 64)
                .placesBlock(placesBlock)
                .categories(parseCategories(categories))
                .build();
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
