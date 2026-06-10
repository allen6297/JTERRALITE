package com.terralite.engine.physics

import com.terralite.engine.entity.ComponentType

object PhysicsComponents {
    @JvmField val TRANSFORM: ComponentType<Transform> = ComponentType.of("terralite:transform")
    @JvmField val PREVIOUS_TRANSFORM: ComponentType<Transform> = ComponentType.of("terralite:previous_transform")
    @JvmField val VELOCITY: ComponentType<Velocity> = ComponentType.of("terralite:velocity")
    @JvmField val COLLIDER: ComponentType<Collider> = ComponentType.of("terralite:collider")
    @JvmField val GROUNDED: ComponentType<Boolean> = ComponentType.of("terralite:grounded")
}
