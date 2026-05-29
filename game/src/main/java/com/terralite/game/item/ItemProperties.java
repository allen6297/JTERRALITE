package com.terralite.game.item;

import com.terralite.core.registry.ResourceId;

import java.util.List;
import java.util.Objects;

public record ItemProperties(
        String displayName,
        float weight,
        int stackSize,
        String placesBlock,
        List<ResourceId> categories
) {
    public ItemProperties {
        Objects.requireNonNull(displayName, "displayName");
        if (weight < 0f) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        if (stackSize <= 0) {
            throw new IllegalArgumentException("Stack size must be positive");
        }
        categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
    }
}
