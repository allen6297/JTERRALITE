package com.terralite.game.item;

import com.terralite.core.registry.ResourceId;

import java.util.List;
import java.util.Objects;

public record ItemProperties(
        float weight,
        List<ResourceId> categories
) {
    public ItemProperties {
        if (weight < 0f) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
        categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
    }
}
