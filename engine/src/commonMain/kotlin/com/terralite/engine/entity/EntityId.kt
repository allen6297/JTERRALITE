package com.terralite.engine.entity

@JvmRecord
data class EntityId(val value: Long) {
    init {
        require(value > 0) { "Entity id must be positive" }
    }

    companion object {
        @JvmStatic fun of(value: Long): EntityId = EntityId(value)
    }
}
