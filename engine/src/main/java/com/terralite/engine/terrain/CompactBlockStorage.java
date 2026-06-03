package com.terralite.engine.terrain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public final class CompactBlockStorage implements BlockStorage {
    private final Map<BlockPos, Integer> blocks = new LinkedHashMap<>();
    private final ToIntFunction<BlockState> stateIds;
    private final IntFunction<BlockState> states;
    private final int airStateId;

    public CompactBlockStorage(
            ToIntFunction<BlockState> stateIds,
            IntFunction<BlockState> states,
            int airStateId
    ) {
        this.stateIds = Objects.requireNonNull(stateIds, "stateIds");
        this.states = Objects.requireNonNull(states, "states");
        this.airStateId = airStateId;
    }

    @Override
    public BlockState get(BlockPos pos) {
        Integer stateId = blocks.get(Objects.requireNonNull(pos, "pos"));
        return stateId == null ? BlockState.AIR : states.apply(stateId);
    }

    public int getStateId(BlockPos pos) {
        return blocks.getOrDefault(Objects.requireNonNull(pos, "pos"), airStateId);
    }

    @Override
    public void set(BlockPos pos, BlockState state) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        int stateId = stateIds.applyAsInt(state);
        setStateId(pos, stateId);
    }

    public void setStateId(BlockPos pos, int stateId) {
        Objects.requireNonNull(pos, "pos");
        if (stateId == airStateId || states.apply(stateId).isAir()) {
            blocks.remove(pos);
            return;
        }
        blocks.put(pos, stateId);
    }

    @Override
    public BlockState remove(BlockPos pos) {
        Integer removed = blocks.remove(Objects.requireNonNull(pos, "pos"));
        return removed == null ? BlockState.AIR : states.apply(removed);
    }

    public int removeStateId(BlockPos pos) {
        Integer removed = blocks.remove(Objects.requireNonNull(pos, "pos"));
        return removed == null ? airStateId : removed;
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
