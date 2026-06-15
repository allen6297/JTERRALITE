package com.terralite.engine.survival

import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.physics.Velocity
import com.terralite.engine.player.PlayerComponents
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World
import kotlin.math.sqrt

class ZombieAISystem : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        val entities = world.entities().entities()

        val players = entities.filter { it.has(PlayerComponents.PLAYER_CONTROLLED) }

        for (zombie in entities) {
            val ai = zombie.get(SurvivalComponents.ZOMBIE_AI) ?: continue
            val zombieTransform = zombie.get(PhysicsComponents.TRANSFORM) ?: continue
            val zombieHealth = zombie.get(SurvivalComponents.HEALTH) ?: continue
            if (zombieHealth.isDead) continue

            ai.ticksSinceLastAttack++

            val target = players
                .filter { it.get(SurvivalComponents.HEALTH)?.isDead == false }
                .minByOrNull { player ->
                    val pt = player.get(PhysicsComponents.TRANSFORM) ?: return@minByOrNull Double.MAX_VALUE
                    distSq(zombieTransform.x, zombieTransform.z, pt.x, pt.z)
                } ?: continue

            val targetTransform = target.get(PhysicsComponents.TRANSFORM) ?: continue
            val dx = targetTransform.x - zombieTransform.x
            val dz = targetTransform.z - zombieTransform.z
            val dist = sqrt(dx * dx + dz * dz)

            if (dist <= ai.attackRange) {
                if (ai.ticksSinceLastAttack >= ai.attackCooldownTicks) {
                    val playerHealth = target.get(SurvivalComponents.HEALTH)
                    if (playerHealth != null) {
                        target.set(SurvivalComponents.HEALTH, playerHealth.damage(ai.attackDamage))
                    }
                    ai.ticksSinceLastAttack = 0
                    zombie.set(SurvivalComponents.ZOMBIE_AI, ai)
                }
                zombie.set(PhysicsComponents.VELOCITY, Velocity(0.0, 0.0, 0.0))
            } else {
                val speed = ai.chaseSpeed
                val nx = dx / dist
                val nz = dz / dist
                zombie.set(PhysicsComponents.VELOCITY, Velocity(nx * speed, 0.0, nz * speed))
            }
        }
    }

    private fun distSq(x1: Double, z1: Double, x2: Double, z2: Double): Double {
        val dx = x2 - x1
        val dz = z2 - z1
        return dx * dx + dz * dz
    }
}
