package com.terralite.engine.player

import com.terralite.engine.input.InputState
import com.terralite.engine.input.MovementIntent
import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.physics.Velocity
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class PlayerInputSystem(private val input: InputState) : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        val intent = MovementIntent.from(input)
        for (entity in world.entities().entities()) {
            if (!entity.has(PlayerComponents.PLAYER_CONTROLLED)) continue
            val player = entity.require(PlayerComponents.PLAYER_CONTROLLED)
            entity.set(PhysicsComponents.VELOCITY, Velocity(
                intent.x * player.movementSpeed,
                intent.y * player.movementSpeed,
                intent.z * player.movementSpeed
            ))
        }
    }
}
