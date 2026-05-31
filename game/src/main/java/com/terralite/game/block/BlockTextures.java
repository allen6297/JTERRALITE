package com.terralite.game.block;

import com.terralite.core.registry.ResourceId;

import java.util.Objects;

public record BlockTextures(ResourceId all, ResourceId top, ResourceId bottom, ResourceId side) {
    public BlockTextures {
        if (all == null && top == null && bottom == null && side == null) {
            throw new IllegalArgumentException("Block textures must define at least one texture");
        }
    }

    public static BlockTextures all(ResourceId texture) {
        return new BlockTextures(Objects.requireNonNull(texture, "texture"), null, null, null);
    }

    public ResourceId textureFor(Face face) {
        Objects.requireNonNull(face, "face");
        return switch (face) {
            case UP -> first(top, side, all);
            case DOWN -> first(bottom, side, all);
            case NORTH, SOUTH, EAST, WEST -> first(side, all, top, bottom);
        };
    }

    private static ResourceId first(ResourceId first, ResourceId second, ResourceId third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        if (third != null) {
            return third;
        }
        throw new IllegalStateException("No texture available for face");
    }

    private static ResourceId first(ResourceId first, ResourceId second, ResourceId third, ResourceId fourth) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        if (third != null) {
            return third;
        }
        if (fourth != null) {
            return fourth;
        }
        throw new IllegalStateException("No texture available for face");
    }

    public enum Face {
        UP,
        DOWN,
        NORTH,
        SOUTH,
        EAST,
        WEST
    }
}
