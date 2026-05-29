package com.terralite.game.biome;

import java.util.Objects;

public record Biome(BiomeProperties properties) {
    public Biome {
        Objects.requireNonNull(properties, "properties");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name = "";
        private int priority = 0;
        private double rarity = 1.0;
        private double temperatureMin = 0.0;
        private double temperatureMax = 1.0;
        private double humidityMin = 0.0;
        private double humidityMax = 1.0;
        private int baseHeight = 64;
        private int heightVariation = 8;
        private String surfaceTop = null;
        private String surfaceMiddle = null;
        private int surfaceMiddleDepth = 3;
        private String surfaceBase = null;

        private Builder() {}

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder rarity(double rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder temperature(double min, double max) {
            this.temperatureMin = min;
            this.temperatureMax = max;
            return this;
        }

        public Builder humidity(double min, double max) {
            this.humidityMin = min;
            this.humidityMax = max;
            return this;
        }

        public Builder baseHeight(int baseHeight) {
            this.baseHeight = baseHeight;
            return this;
        }

        public Builder heightVariation(int heightVariation) {
            this.heightVariation = heightVariation;
            return this;
        }

        public Builder surfaceTop(String blockId) {
            this.surfaceTop = blockId;
            return this;
        }

        public Builder surfaceMiddle(String blockId) {
            this.surfaceMiddle = blockId;
            return this;
        }

        public Builder surfaceMiddleDepth(int depth) {
            this.surfaceMiddleDepth = depth;
            return this;
        }

        public Builder surfaceBase(String blockId) {
            this.surfaceBase = blockId;
            return this;
        }

        public Biome build() {
            return new Biome(new BiomeProperties(
                    name, priority, rarity,
                    temperatureMin, temperatureMax,
                    humidityMin, humidityMax,
                    baseHeight, heightVariation,
                    surfaceTop, surfaceMiddle, surfaceMiddleDepth, surfaceBase
            ));
        }
    }
}
