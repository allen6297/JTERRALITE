package com.terralite.engine.survival

data class ItemStack(val itemId: String, val count: Int) {
    init {
        require(itemId.isNotBlank()) { "Item id cannot be blank" }
        require(count > 0) { "Item count must be positive" }
    }

    fun withCount(n: Int): ItemStack = copy(count = n)
}
