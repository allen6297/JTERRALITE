package com.terralite.engine.camera;

import com.terralite.engine.entity.EntityId;

import java.util.Objects;

public record CameraTarget(EntityId entityId) {
    public CameraTarget {
        Objects.requireNonNull(entityId, "entityId");
    }
}
