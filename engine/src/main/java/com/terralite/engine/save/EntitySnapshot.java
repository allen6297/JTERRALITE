package com.terralite.engine.save;

import com.terralite.engine.entity.EntityId;

import java.util.Objects;

public record EntitySnapshot(EntityId id) {
    public EntitySnapshot {
        Objects.requireNonNull(id, "id");
    }
}
