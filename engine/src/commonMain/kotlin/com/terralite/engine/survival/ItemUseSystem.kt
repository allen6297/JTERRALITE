package com.terralite.engine.survival

import com.terralite.engine.input.InputActions
import com.terralite.engine.input.InputState
import com.terralite.engine.player.PlayerComponents
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.world.World

class ItemUseSystem(
    private val input: InputState,
    private val itemEffects: Map<String, ItemEffect> = defaultEffects()
) : WorldSimulationSystem {

    private var hotbarIndex: Int = 0
    private var useWasPressed: Boolean = false
    private var nextWasPressed: Boolean = false
    private var prevWasPressed: Boolean = false

    override fun tick(world: World, tick: SimulationTick) {
        val usePressed = input.isPressed(InputActions.USE_ITEM)
        val nextPressed = input.isPressed(InputActions.HOTBAR_NEXT)
        val prevPressed = input.isPressed(InputActions.HOTBAR_PREV)

        for (entity in world.entities().entities()) {
            if (!entity.has(PlayerComponents.PLAYER_CONTROLLED)) continue
            val health = entity.get(SurvivalComponents.HEALTH) ?: continue
            if (health.isDead) continue

            val inv = entity.get(LootComponents.INVENTORY) ?: continue
            if (inv.items.isEmpty()) continue

            if (nextPressed && !nextWasPressed) {
                hotbarIndex = (hotbarIndex + 1) % inv.items.size
            }
            if (prevPressed && !prevWasPressed) {
                hotbarIndex = (hotbarIndex - 1 + inv.items.size) % inv.items.size
            }
            hotbarIndex = hotbarIndex.coerceIn(0, inv.items.size - 1)

            if (usePressed && !useWasPressed) {
                val selected = inv.items[hotbarIndex]
                val effect = itemEffects[selected.itemId] ?: continue

                var newHealth = health
                var newHunger = entity.get(SurvivalComponents.HUNGER)

                newHealth = effect.applyHealth(newHealth)
                if (newHunger != null) newHunger = effect.applyHunger(newHunger)

                entity.set(SurvivalComponents.HEALTH, newHealth)
                if (newHunger != null) entity.set(SurvivalComponents.HUNGER, newHunger)
                entity.set(LootComponents.INVENTORY, inv.remove(selected.itemId, 1))
            }
        }

        useWasPressed = usePressed
        nextWasPressed = nextPressed
        prevWasPressed = prevPressed
    }

    companion object {
        fun defaultEffects(): Map<String, ItemEffect> = mapOf(
            "terralite:raw_meat" to ItemEffect(hungerRestore = 20f),
            "terralite:cooked_meat" to ItemEffect(hungerRestore = 40f),
            "terralite:bandage" to ItemEffect(healthRestore = 25f)
        )
    }
}

data class ItemEffect(
    val healthRestore: Float = 0f,
    val hungerRestore: Float = 0f
) {
    fun applyHealth(h: Health): Health =
        if (healthRestore > 0f) h.copy(current = (h.current + healthRestore).coerceAtMost(h.max)) else h

    fun applyHunger(h: Hunger): Hunger =
        if (hungerRestore > 0f) h.eat(hungerRestore) else h
}
