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
        private String displayName = "";
        private float hardness = 1.0f;
        private float resistance = 1.0f;
        private boolean solid = true;
        private boolean transparent;
        private boolean requiresTool;
        private String material = "stone";
        private String soundType = "stone";
        private BlockTextures textures;
        private BlockModel model = BlockModel.CUBE_ALL;
        private BlockOccupancy occupancy = BlockOccupancy.SINGLE;
        private BlockStateDefinition stateDefinition = BlockStateDefinition.EMPTY;
        private final List<BlockModelVariant> modelVariants = new ArrayList<>();
        private final List<ResourceId> categories = new ArrayList<>();

        private Builder() {}

        public Builder displayName(String displayName) {
            this.displayName = Objects.requireNonNull(displayName, "displayName");
            return this;
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

        public Builder textures(BlockTextures textures) {
            this.textures = Objects.requireNonNull(textures, "textures");
            return this;
        }

        public Builder model(BlockModel model) {
            this.model = Objects.requireNonNull(model, "model");
            return this;
        }

        public Builder model(String model) {
            return model(BlockModel.of(model));
        }

        public Builder occupancy(BlockOccupancy occupancy) {
            this.occupancy = Objects.requireNonNull(occupancy, "occupancy");
            return this;
        }

        public Builder stateDefinition(BlockStateDefinition stateDefinition) {
            this.stateDefinition = Objects.requireNonNull(stateDefinition, "stateDefinition");
            return this;
        }

        public Builder modelVariant(BlockModelVariant variant) {
            modelVariants.add(Objects.requireNonNull(variant, "variant"));
            return this;
        }

        public Builder modelVariants(Collection<BlockModelVariant> variants) {
            this.modelVariants.clear();
            this.modelVariants.addAll(Objects.requireNonNull(variants, "variants"));
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
                displayName,
                hardness,
                resistance,
                solid,
                transparent,
                requiresTool,
                material,
                soundType,
                List.copyOf(categories),
                textures,
                model,
                occupancy,
                stateDefinition,
                List.copyOf(modelVariants)
            ));
        }
    }
}
