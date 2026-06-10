package com.terralite.core.registry

/** [Registry] that accepts new entries and can be sealed into a [FrozenRegistry]. */
interface MutableRegistry<T> : Registry<T> {

    fun register(id: ResourceId, value: T): T

    fun register(key: ResourceKey<T>, value: T): T {
        require(key().id == key.registry.id) {
            "Resource key belongs to registry ${key.registry.id}, not ${id()}"
        }
        return register(key.id, value)
    }

    fun replace(id: ResourceId, value: T): T

    fun freeze(): FrozenRegistry<T>
}
