package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;

import java.util.Objects;

public record CollisionPair(Entity first, Entity second) {
    public CollisionPair {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first == second || first.id().equals(second.id())) {
            throw new IllegalArgumentException("Collision pair requires two distinct entities");
        }
    }
}
