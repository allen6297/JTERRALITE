package com.terralite.engine.physics;

public record Collider(double halfWidth, double halfHeight, double halfDepth) {
    public Collider {
        if (halfWidth < 0.0) {
            throw new IllegalArgumentException("Collider half width cannot be negative");
        }
        if (halfHeight < 0.0) {
            throw new IllegalArgumentException("Collider half height cannot be negative");
        }
        if (halfDepth < 0.0) {
            throw new IllegalArgumentException("Collider half depth cannot be negative");
        }
    }

    public Aabb boundsAt(Transform transform) {
        return new Aabb(
            transform.x() - halfWidth,
            transform.y() - halfHeight,
            transform.z() - halfDepth,
            transform.x() + halfWidth,
            transform.y() + halfHeight,
            transform.z() + halfDepth
        );
    }
}
