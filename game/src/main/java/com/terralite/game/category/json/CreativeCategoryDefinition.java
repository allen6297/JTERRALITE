package com.terralite.game.category.json;

import com.terralite.core.registry.ResourceId;
import com.terralite.game.category.CreativeCategory;

import java.util.List;

public record CreativeCategoryDefinition(
        String title,
        String icon,
        List<String> entries
) {
    public CreativeCategory toCategory() {
        return CreativeCategory.builder()
                .title(defaultString(title, "Category"))
                .icon(defaultString(icon, "terralite:air"))
                .entries(parseIds(entries))
                .build();
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static List<ResourceId> parseIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .map(ResourceId::id)
                .toList();
    }
}
