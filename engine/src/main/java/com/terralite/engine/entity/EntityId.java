package com.terralite.engine.entity;

public record EntityId(long value) {
    public EntityId {
        if (value <= 0) {
            throw new IllegalArgumentException("Entity id must be positive");
        }
    }

    public static EntityId of(long value) {
        return new EntityId(value);
    }
}
