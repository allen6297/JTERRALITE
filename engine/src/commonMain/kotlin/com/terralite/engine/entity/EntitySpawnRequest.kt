package com.terralite.engine.entity

import com.terralite.engine.physics.Transform
import com.terralite.engine.physics.Velocity
import com.terralite.engine.player.PlayerControlled

class EntitySpawnRequest {
    var transform: Transform? = null
        private set
    var velocity: Velocity? = null
        private set
    var playerControlled: PlayerControlled? = null
        private set

    fun transform(transform: Transform): EntitySpawnRequest {
        this.transform = transform
        return this
    }

    fun velocity(velocity: Velocity): EntitySpawnRequest {
        this.velocity = velocity
        return this
    }

    fun playerControlled(playerControlled: PlayerControlled): EntitySpawnRequest {
        this.playerControlled = playerControlled
        return this
    }

    companion object {
        @JvmStatic fun create(): EntitySpawnRequest = EntitySpawnRequest()
    }
}
