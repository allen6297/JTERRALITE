package com.terralite.engine.terrain;

import java.util.Collection;

public interface BlockStorage {
    BlockState get(BlockPos pos);

    void set(BlockPos pos, BlockState state);

    BlockState remove(BlockPos pos);

    boolean contains(BlockPos pos);

    Collection<BlockPos> positions();

    int size();
}
