package com.terralite.game.block;

import com.terralite.engine.terrain.BlockPos;

import java.util.Locale;
import java.util.Objects;

public enum BlockDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public static BlockDirection parse(String value) {
        Objects.requireNonNull(value, "value");
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public BlockPos rotate(BlockPos offset) {
        Objects.requireNonNull(offset, "offset");
        return switch (this) {
            case NORTH -> offset;
            case EAST -> BlockPos.of(-offset.z(), offset.y(), offset.x());
            case SOUTH -> BlockPos.of(-offset.x(), offset.y(), -offset.z());
            case WEST -> BlockPos.of(offset.z(), offset.y(), -offset.x());
        };
    }
}
