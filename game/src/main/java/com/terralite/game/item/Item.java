package com.terralite.game.item;

import java.util.Objects;

public record Item(ItemProperties properties) {
    public Item{
        Objects.requireNonNull(properties, "Item cannot be null");
    }
    public static Builder builder() {return new Builder();}
    public static final class Builder {
        //properties here
        public float weight = 1.0f;

        private Builder() {}
        //prop builders here
        public Builder weight(float weight) {

            this.weight = weight;
            return this;
        }
        public Item build() {return new Item(new ItemProperties(
                //properties here tc
                weight
        ));
        }
    }

}
