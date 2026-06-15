package com.terralite.engine.survival

import com.terralite.engine.entity.ComponentType

data class Health(val current: Float, val max: Float) {
    val isDead: Boolean get() = current <= 0f
    fun damage(amount: Float): Health = copy(current = (current - amount).coerceAtLeast(0f))
}

data class Hunger(val current: Float, val max: Float) {
    val isStarving: Boolean get() = current <= 0f
    fun drain(amount: Float): Hunger = copy(current = (current - amount).coerceAtLeast(0f))
    fun eat(amount: Float): Hunger = copy(current = (current + amount).coerceAtMost(max))
}

data class ZombieAI(
    val chaseSpeed: Double = 2.5,
    val attackRange: Double = 1.2,
    val attackDamage: Float = 10f,
    val attackCooldownTicks: Int = 20,
    var ticksSinceLastAttack: Int = 0
)

object SurvivalComponents {
    @JvmField val HEALTH: ComponentType<Health> = ComponentType.of("terralite:health")
    @JvmField val HUNGER: ComponentType<Hunger> = ComponentType.of("terralite:hunger")
    @JvmField val ZOMBIE_AI: ComponentType<ZombieAI> = ComponentType.of("terralite:zombie_ai")
}
