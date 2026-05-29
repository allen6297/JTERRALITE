package com.terralite.game.item.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.item.Item;

import java.util.List;

public record ItemDefinition(
        @JsonProperty(defaultValue = "1.0")
        float weight,
        List<String> categories
        ) {
    public Item toItem() {
        return Item.builder()
                .weight(weight)
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
