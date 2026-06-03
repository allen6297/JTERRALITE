package com.terralite.engine.save;

import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;

import java.util.Objects;
import java.util.OptionalInt;

public record BlockSnapshot(BlockPos pos, BlockState state, Integer stateId) {
    public BlockSnapshot {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
    }

    public OptionalInt stateIdOptional() {
        return stateId == null ? OptionalInt.empty() : OptionalInt.of(stateId);
    }
}
