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
        private float weight = 1.0f;
        private final List<ResourceId> categories = new ArrayList<>();

        private Builder() {}

        public Builder weight(float weight) {

            this.weight = weight;
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

        public Item build() {return new Item(new ItemProperties(
                weight,
                List.copyOf(categories)
        ));
        }
    }

}
