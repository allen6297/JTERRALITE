package com.terralite.engine.survival

import com.terralite.engine.entity.ComponentType

data class LootTable(val entries: List<LootEntry>)

data class LootEntry(
    val item: ItemStack,
    val chance: Float,   // 0.0–1.0
    val minCount: Int = 1,
    val maxCount: Int = 1
)

data class Inventory(val items: List<ItemStack> = emptyList()) {
    fun add(stack: ItemStack): Inventory {
        val existing = items.indexOfFirst { it.itemId == stack.itemId }
        return if (existing >= 0) {
            val merged = items.toMutableList()
            merged[existing] = merged[existing].copy(count = merged[existing].count + stack.count)
            copy(items = merged)
        } else {
            copy(items = items + stack)
        }
    }

    fun remove(itemId: String, count: Int): Inventory {
        val idx = items.indexOfFirst { it.itemId == itemId }
        if (idx < 0) return this
        val after = items[idx].count - count
        val updated = items.toMutableList()
        if (after <= 0) updated.removeAt(idx) else updated[idx] = updated[idx].copy(count = after)
        return copy(items = updated)
    }

    fun countOf(itemId: String): Int = items.firstOrNull { it.itemId == itemId }?.count ?: 0
}

/** Marks an entity as a dropped item on the ground. */
data class DroppedItem(val stack: ItemStack)

object LootComponents {
    @JvmField val LOOT_TABLE: ComponentType<LootTable> = ComponentType.of("terralite:loot_table")
    @JvmField val INVENTORY: ComponentType<Inventory> = ComponentType.of("terralite:inventory")
    @JvmField val DROPPED_ITEM: ComponentType<DroppedItem> = ComponentType.of("terralite:dropped_item")
}
