package com.terralite.game.block;

import com.terralite.core.registry.ResourceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record Block(BlockProperties properties) {
    public Block {
        Objects.requireNonNull(properties, "Block properties cannot be null");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float hardness = 1.0f;
        private float resistance = 1.0f;
        private boolean solid = true;
        private boolean transparent;
        private boolean requiresTool;
        private String material = "stone";
        private String soundType = "stone";
        private final List<ResourceId> categories = new ArrayList<>();

        private Builder() {}

        public Builder hardness(float hardness) {
            this.hardness = hardness;
            return this;
        }

        public Builder resistance(float resistance) {
            this.resistance = resistance;
            return this;
        }

        public Builder solid(boolean solid) {
            this.solid = solid;
            return this;
        }

        public Builder transparent(boolean transparent) {
            this.transparent = transparent;
            return this;
        }

        public Builder requiresTool(boolean requiresTool) {
            this.requiresTool = requiresTool;
            return this;
        }

        public Builder material(String material) {
            this.material = Objects.requireNonNull(material, "material");
            return this;
        }

        public Builder soundType(String soundType) {
            this.soundType = Objects.requireNonNull(soundType, "soundType");
            return this;
        }

        public Builder category(ResourceId category) {
            categories.add(Objects.requireNonNull(category, "category"));
            return this;
        }

        public Builder category(String category) {
            return category(ResourceId.id(category));
        }

        public Builder categories(Collection<ResourceId> categories) {
            this.categories.clear();
            this.categories.addAll(Objects.requireNonNull(categories, "categories"));
            return this;
        }

        public Block build() {
            return new Block(new BlockProperties(
                hardness,
                resistance,
                solid,
                transparent,
                requiresTool,
                material,
                soundType,
                List.copyOf(categories)
            ));
        }
    }
}
