package com.terralite.engine.terrain;

import com.terralite.core.registry.ResourceId;

import java.util.Objects;

public record BlockState(ResourceId id) {
    public static final BlockState AIR = new BlockState(ResourceId.id("terralite:air"));

    public BlockState {
        Objects.requireNonNull(id, "id");
    }

    public static BlockState of(String id) {
        return new BlockState(ResourceId.id(id));
    }

    public boolean isAir() {
        return this.equals(AIR);
    }
}
