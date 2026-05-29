package com.terralite.game.item;

import com.terralite.core.registry.ResourceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record Item(ItemProperties properties) {
    public Item{
        Objects.requireNonNull(properties, "Item cannot be null");
    }
    public static Builder builder() {return new Builder();}
    public static final class Builder {
        private String displayName = "";
        private float weight = 1.0f;
        private int stackSize = 64;
        private String placesBlock = null;
        private final List<ResourceId> categories = new ArrayList<>();

        private Builder() {}

        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
        }

        public Builder weight(float weight) {
            this.weight = weight;
            return this;
        }

        public Builder stackSize(int stackSize) {
            this.stackSize = stackSize;
            return this;
        }

        public Builder placesBlock(String placesBlock) {
            this.placesBlock = placesBlock;
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

        public Item build() {
            return new Item(new ItemProperties(
                    displayName,
                    weight,
                    stackSize,
                    placesBlock,
                    List.copyOf(categories)
            ));
        }
    }

}
