package com.terralite.engine.survival

import com.terralite.engine.entity.EntityManager
import com.terralite.engine.physics.Collider
import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.physics.Transform
import com.terralite.engine.physics.Velocity

object ZombieSpawner {
    fun spawn(entities: EntityManager, x: Double, y: Double, z: Double) {
        val zombie = entities.create()
        zombie.set(PhysicsComponents.TRANSFORM, Transform(x, y, z))
        zombie.set(PhysicsComponents.VELOCITY, Velocity(0.0, 0.0, 0.0))
        zombie.set(PhysicsComponents.COLLIDER, Collider(halfWidth = 0.3, halfHeight = 0.9, halfDepth = 0.3))
        zombie.set(SurvivalComponents.HEALTH, Health(current = 50f, max = 50f))
        zombie.set(SurvivalComponents.ZOMBIE_AI, ZombieAI())
        zombie.set(LootComponents.LOOT_TABLE, LootTable(listOf(
            LootEntry(ItemStack("terralite:raw_meat", 1), chance = 0.6f, minCount = 1, maxCount = 2),
            LootEntry(ItemStack("terralite:bandage", 1), chance = 0.2f)
        )))
        entities.add(zombie)
    }

    fun spawnPlayer(entities: EntityManager, x: Double, y: Double, z: Double) {
        val player = entities.create()
        player.set(PhysicsComponents.TRANSFORM, Transform(x, y, z))
        player.set(PhysicsComponents.VELOCITY, Velocity(0.0, 0.0, 0.0))
        player.set(PhysicsComponents.COLLIDER, Collider(halfWidth = 0.3, halfHeight = 0.9, halfDepth = 0.3))
        player.set(SurvivalComponents.HEALTH, Health(current = 100f, max = 100f))
        player.set(SurvivalComponents.HUNGER, Hunger(current = 100f, max = 100f))
        player.set(LootComponents.INVENTORY, Inventory())
        entities.add(player)
    }
}
