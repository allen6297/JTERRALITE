package com.terralite.runtime.terrain;

import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;

public record BlockPlacement(BlockPos pos, BlockState state) {}
