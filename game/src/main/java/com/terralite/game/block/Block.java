package com.terralite.game.block;

import java.util.Objects;

public record Block(BlockProperties properties) {
    public Block {
        Objects.requireNonNull(properties, "properties");
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

        private Builder() {
        }

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

        public Block build() {
            return new Block(new BlockProperties(
                hardness,
                resistance,
                solid,
                transparent,
                requiresTool,
                material,
                soundType
            ));
        }
    }
}
