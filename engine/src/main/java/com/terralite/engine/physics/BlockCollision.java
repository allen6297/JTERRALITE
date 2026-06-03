package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.terrain.BlockPos;

import java.util.Objects;

public record BlockCollision(Entity entity, BlockPos blockPos, Aabb blockBounds) {
    public BlockCollision {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(blockPos, "blockPos");
        Objects.requireNonNull(blockBounds, "blockBounds");
    }
}
