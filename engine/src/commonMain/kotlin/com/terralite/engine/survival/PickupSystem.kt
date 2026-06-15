package com.terralite.engine.survival

import com.terralite.engine.entity.EntityId
import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.player.PlayerComponents
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World
import kotlin.math.sqrt

class PickupSystem(private val pickupRadius: Double = 1.0) : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        val entities = world.entities().entities()
        val players = entities.filter { it.has(PlayerComponents.PLAYER_CONTROLLED) }
        val drops = entities.filter { it.has(LootComponents.DROPPED_ITEM) }
        if (players.isEmpty() || drops.isEmpty()) return

        val pickedUp = mutableListOf<EntityId>()

        for (drop in drops) {
            val dropPos = drop.get(PhysicsComponents.TRANSFORM) ?: continue
            val dropped = drop.get(LootComponents.DROPPED_ITEM) ?: continue

            for (player in players) {
                if (player.get(SurvivalComponents.HEALTH)?.isDead == true) continue
                val playerPos = player.get(PhysicsComponents.TRANSFORM) ?: continue

                val dx = playerPos.x - dropPos.x
                val dz = playerPos.z - dropPos.z
                val dist = sqrt(dx * dx + dz * dz)
                if (dist > pickupRadius) continue

                val inv = player.get(LootComponents.INVENTORY) ?: Inventory()
                player.set(LootComponents.INVENTORY, inv.add(dropped.stack))
                pickedUp += drop.id
                break
            }
        }

        for (id in pickedUp) world.entities().remove(id)
    }
}
