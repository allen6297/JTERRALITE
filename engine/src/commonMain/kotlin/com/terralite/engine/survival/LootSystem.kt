package com.terralite.engine.survival

import com.terralite.engine.entity.EntityId
import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.physics.Transform
import com.terralite.engine.physics.Velocity
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World
import kotlin.random.Random

class LootSystem(private val random: Random = Random.Default) : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        val toRemove = mutableListOf<EntityId>()

        for (entity in world.entities().entities()) {
            val health = entity.get(SurvivalComponents.HEALTH) ?: continue
            if (!health.isDead) continue
            val lootTable = entity.get(LootComponents.LOOT_TABLE) ?: continue

            val transform = entity.get(PhysicsComponents.TRANSFORM) ?: Transform.ORIGIN

            for (entry in lootTable.entries) {
                if (random.nextFloat() > entry.chance) continue
                val count = if (entry.minCount == entry.maxCount) entry.minCount
                            else random.nextInt(entry.minCount, entry.maxCount + 1)
                spawnDrop(world, transform, entry.item.withCount(count))
            }

            toRemove += entity.id
        }

        for (id in toRemove) world.entities().remove(id)
    }

    private fun spawnDrop(world: World, at: Transform, stack: ItemStack) {
        val drop = world.entities().create()
        drop.set(PhysicsComponents.TRANSFORM, at)
        drop.set(PhysicsComponents.VELOCITY, Velocity(0.0, 0.0, 0.0))
        drop.set(LootComponents.DROPPED_ITEM, DroppedItem(stack))
        world.entities().add(drop)
    }
}
