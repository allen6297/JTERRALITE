package com.terralite.engine.entity

import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.player.PlayerComponents
import com.terralite.engine.world.World

class EntitySpawner(private val world: World) {
    fun spawn(request: EntitySpawnRequest): Entity {
        val entity = world.entities().create()
        request.transform?.let { entity.set(PhysicsComponents.TRANSFORM, it) }
        request.velocity?.let { entity.set(PhysicsComponents.VELOCITY, it) }
        request.playerControlled?.let { entity.set(PlayerComponents.PLAYER_CONTROLLED, it) }
        return entity
    }

    fun despawn(id: EntityId): Entity = world.entities().remove(id)
}
