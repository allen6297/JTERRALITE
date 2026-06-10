package com.terralite.engine.terrain

import com.terralite.core.registry.ResourceId

@JvmRecord
data class BlockState(val id: ResourceId, val properties: Map<String, String>) {
    constructor(id: ResourceId) : this(id, emptyMap())

    init {
        @Suppress("SENSELESS_COMPARISON")
        require(id != null) { "id" }
    }

    companion object {
        @JvmField val AIR: BlockState = BlockState(ResourceId.id("terralite:air"))

        @JvmStatic fun of(id: String): BlockState = BlockState(ResourceId.id(id))
    }

    fun with(property: String, value: String): BlockState {
        require(property.isNotBlank()) { "Block state property cannot be blank" }
        require(value.isNotBlank()) { "Block state value cannot be blank" }
        val next = LinkedHashMap(properties)
        next[property] = value
        return BlockState(id, next)
    }

    fun property(property: String): String? = properties[property]

    fun isAir(): Boolean = this == AIR
}
