package com.terralite.engine.terrain;

import java.util.Collection;
import java.util.OptionalInt;

public interface BlockStorage {
    BlockState get(BlockPos pos);

    void set(BlockPos pos, BlockState state);

    BlockState remove(BlockPos pos);

    default OptionalInt stateId(BlockPos pos) {
        return OptionalInt.empty();
    }

    boolean contains(BlockPos pos);

    Collection<BlockPos> positions();

    int size();
}
