package com.terralite.engine.physics;

import com.terralite.engine.entity.ComponentType;

public final class PhysicsComponents {
    public static final ComponentType<Transform> TRANSFORM = ComponentType.of("terralite:transform", Transform.class);
    public static final ComponentType<Transform> PREVIOUS_TRANSFORM = ComponentType.of("terralite:previous_transform", Transform.class);
    public static final ComponentType<Velocity> VELOCITY = ComponentType.of("terralite:velocity", Velocity.class);
    public static final ComponentType<Collider> COLLIDER = ComponentType.of("terralite:collider", Collider.class);

    private PhysicsComponents() {
    }
}
