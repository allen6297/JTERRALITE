package com.terralite.game.block;

import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record BlockOccupancy(List<BlockPos> offsets, String rotatesWith) {
    public static final BlockOccupancy SINGLE = new BlockOccupancy(List.of(BlockPos.of(0, 0, 0)));

    public BlockOccupancy(List<BlockPos> offsets) {
        this(offsets, null);
    }

    public BlockOccupancy {
        offsets = List.copyOf(Objects.requireNonNull(offsets, "offsets"));
        if (offsets.isEmpty()) {
            throw new IllegalArgumentException("Block occupancy must contain at least one offset");
        }
        if (!offsets.contains(BlockPos.of(0, 0, 0))) {
            throw new IllegalArgumentException("Block occupancy must include the origin offset");
        }
        if (new HashSet<>(offsets).size() != offsets.size()) {
            throw new IllegalArgumentException("Block occupancy offsets cannot contain duplicates");
        }
        if (rotatesWith != null && rotatesWith.isBlank()) {
            throw new IllegalArgumentException("Block occupancy rotation property cannot be blank");
        }
    }

    public List<BlockPos> offsetsFor(BlockState state) {
        Objects.requireNonNull(state, "state");
        if (rotatesWith == null) {
            return offsets;
        }
        String directionValue = state.property(rotatesWith);
        if (directionValue == null || directionValue.isBlank()) {
            return offsets;
        }
        BlockDirection direction = BlockDirection.parse(directionValue);
        return offsets.stream()
                .map(direction::rotate)
                .toList();
    }
}
