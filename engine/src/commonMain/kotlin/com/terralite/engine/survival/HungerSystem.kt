package com.terralite.engine.survival

import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class HungerSystem(
    private val drainPerTick: Float = 0.005f,
    private val starveDamagePerTick: Float = 0.5f
) : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        for (entity in world.entities().entities()) {
            val hunger = entity.get(SurvivalComponents.HUNGER) ?: continue
            val health = entity.get(SurvivalComponents.HEALTH) ?: continue
            if (health.isDead) continue

            val newHunger = hunger.drain(drainPerTick)
            entity.set(SurvivalComponents.HUNGER, newHunger)

            if (newHunger.isStarving) {
                entity.set(SurvivalComponents.HEALTH, health.damage(starveDamagePerTick))
            }
        }
    }
}
