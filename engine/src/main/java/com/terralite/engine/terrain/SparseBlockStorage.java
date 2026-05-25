package com.terralite.engine.terrain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SparseBlockStorage implements BlockStorage {
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();

    @Override
    public BlockState get(BlockPos pos) {
        return blocks.getOrDefault(Objects.requireNonNull(pos, "pos"), BlockState.AIR);
    }

    @Override
    public void set(BlockPos pos, BlockState state) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (state.isAir()) {
            blocks.remove(pos);
            return;
        }
        blocks.put(pos, state);
    }

    @Override
    public BlockState remove(BlockPos pos) {
        BlockState removed = blocks.remove(Objects.requireNonNull(pos, "pos"));
        return removed == null ? BlockState.AIR : removed;
    }

    @Override
    public boolean contains(BlockPos pos) {
        return blocks.containsKey(Objects.requireNonNull(pos, "pos"));
    }

    @Override
    public Collection<BlockPos> positions() {
        return List.copyOf(blocks.keySet());
    }

    @Override
    public int size() {
        return blocks.size();
    }
}
