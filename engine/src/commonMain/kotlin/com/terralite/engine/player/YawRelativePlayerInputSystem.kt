package com.terralite.engine.player

import com.terralite.engine.camera.Camera
import com.terralite.engine.input.InputActions
import com.terralite.engine.input.InputState
import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.physics.Velocity
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

/**
 * Converts WASD input into world-space velocity based on the camera's current yaw,
 * so the player always moves in the direction they are facing.
 */
class YawRelativePlayerInputSystem(
    private val input: InputState,
    private val camera: Camera
) : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        val yawRad = Math.toRadians(camera.yaw())

        val fwdX = -Math.sin(yawRad)
        val fwdZ = -Math.cos(yawRad)
        val rightX = Math.cos(yawRad)
        val rightZ = -Math.sin(yawRad)

        var forward = 0.0; var strafe = 0.0
        if (input.isPressed(InputActions.MOVE_FORWARD)) forward += 1.0
        if (input.isPressed(InputActions.MOVE_BACK))    forward -= 1.0
        if (input.isPressed(InputActions.MOVE_RIGHT))   strafe  += 1.0
        if (input.isPressed(InputActions.MOVE_LEFT))    strafe  -= 1.0

        var worldDx = forward * fwdX + strafe * rightX
        var worldDz = forward * fwdZ + strafe * rightZ

        val horizLen = Math.sqrt(worldDx * worldDx + worldDz * worldDz)
        if (horizLen > 1.0) { worldDx /= horizLen; worldDz /= horizLen }

        for (entity in world.entities().entities()) {
            if (!entity.has(PlayerComponents.PLAYER_CONTROLLED)) continue
            val player = entity.require(PlayerComponents.PLAYER_CONTROLLED)
            var speed = player.movementSpeed
            if (input.isPressed(InputActions.SPRINT)) speed *= SPRINT_MULTIPLIER

            val currentVy = if (entity.has(PhysicsComponents.VELOCITY)) entity.require(PhysicsComponents.VELOCITY).y else 0.0
            val grounded = entity.get(PhysicsComponents.GROUNDED) ?: false
            val vy = if (input.isPressed(InputActions.JUMP) && grounded) JUMP_SPEED else currentVy

            entity.set(PhysicsComponents.VELOCITY, Velocity(worldDx * speed, vy, worldDz * speed))
        }
    }

    companion object {
        private const val SPRINT_MULTIPLIER = 2.0
        private const val JUMP_SPEED = 8.0
    }
}
