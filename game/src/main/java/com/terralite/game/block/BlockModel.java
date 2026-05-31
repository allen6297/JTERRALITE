package com.terralite.game.block;

import com.terralite.core.registry.ResourceId;

import java.util.Objects;

public record BlockModel(ResourceId id) {
    public static final BlockModel CUBE_ALL = new BlockModel(ResourceId.id("terralite:block/cube_all"));

    public BlockModel {
        Objects.requireNonNull(id, "id");
    }

    public static BlockModel of(String id) {
        return new BlockModel(ResourceId.id(id));
    }
}
