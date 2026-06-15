package com.terralite.engine.survival

import com.terralite.engine.physics.PhysicsComponents
import com.terralite.engine.player.PlayerComponents
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.time.GameClock
import com.terralite.engine.world.World
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Spawns a wave of zombies at the start of each night. Wave size scales with day number.
 * Night is defined as dayProgress outside [dawningAt, duskAt].
 */
class ZombieSpawnSystem(
    private val clock: GameClock,
    private val baseWaveSize: Int = 3,
    private val waveGrowthPerDay: Int = 2,
    private val spawnRadius: Double = 24.0,
    private val dawningAt: Double = 0.25,
    private val duskAt: Double = 0.75,
    private val random: Random = Random.Default
) : WorldSimulationSystem {

    private var lastDay: Long = -1
    private var nightWaveSpawned: Boolean = false

    override fun tick(world: World, tick: SimulationTick) {
        val day = clock.day()
        val progress = clock.dayProgress()
        val isNight = progress < dawningAt || progress > duskAt

        if (day != lastDay) {
            lastDay = day
            nightWaveSpawned = false
        }

        if (isNight && !nightWaveSpawned) {
            nightWaveSpawned = true
            spawnWave(world, day)
        }
    }

    private fun spawnWave(world: World, day: Long) {
        val count = baseWaveSize + (day * waveGrowthPerDay).toInt().coerceAtMost(100)
        val center = findPlayerCenter(world)

        val lootTable = LootTable(listOf(
            LootEntry(ItemStack("terralite:raw_meat", 1), chance = 0.6f, minCount = 1, maxCount = 2),
            LootEntry(ItemStack("terralite:bandage", 1), chance = 0.2f)
        ))

        repeat(count) {
            val angle = random.nextDouble() * 2 * PI
            val dist = spawnRadius * (0.75 + random.nextDouble() * 0.25)
            val x = center.first + cos(angle) * dist
            val z = center.second + sin(angle) * dist
            val zombie = world.entities().create()
            zombie.set(com.terralite.engine.physics.PhysicsComponents.TRANSFORM,
                com.terralite.engine.physics.Transform(x, 1.0, z))
            zombie.set(com.terralite.engine.physics.PhysicsComponents.VELOCITY,
                com.terralite.engine.physics.Velocity(0.0, 0.0, 0.0))
            zombie.set(com.terralite.engine.physics.PhysicsComponents.COLLIDER,
                com.terralite.engine.physics.Collider(0.3, 0.9, 0.3))
            zombie.set(SurvivalComponents.HEALTH, Health(current = 50f, max = 50f))
            zombie.set(SurvivalComponents.ZOMBIE_AI, ZombieAI())
            zombie.set(LootComponents.LOOT_TABLE, lootTable)
            world.entities().add(zombie)
        }
    }

    private fun findPlayerCenter(world: World): Pair<Double, Double> {
        val players = world.entities().entities()
            .filter { it.has(PlayerComponents.PLAYER_CONTROLLED) }
        if (players.isEmpty()) return Pair(0.0, 0.0)
        val transforms = players.mapNotNull { it.get(PhysicsComponents.TRANSFORM) }
        if (transforms.isEmpty()) return Pair(0.0, 0.0)
        return Pair(transforms.map { it.x }.average(), transforms.map { it.z }.average())
    }
}
