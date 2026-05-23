package com.terralite.game.item;

public record ItemProperties(
        float Weight
) {
    public ItemProperties {
        if (Weight < 0f) {
            throw new IllegalArgumentException("Weight cannot be negative");
        }
    }
}