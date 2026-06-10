package com.terralite.engine.camera

import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.physics.Transform
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class FollowCameraSystem(
    private val camera: Camera,
    private val target: CameraTarget,
    private val offset: Transform
) : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        val entity = world.entities().require(target.entityId)
        val targetTransform = entity.require(PhysicsComponents.TRANSFORM)
        camera.setTransform(targetTransform.translate(offset.x, offset.y, offset.z))
    }
}
