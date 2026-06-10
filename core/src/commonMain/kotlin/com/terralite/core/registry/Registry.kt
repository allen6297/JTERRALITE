package com.terralite.core.registry

/**
 * Read-only view of a named registry that maps [ResourceId]s to values of type [T].
 */
interface Registry<T> {

    fun key(): RegistryKey<T>

    fun id(): ResourceId = key().id

    /** Returns the value for [id], or `null` if not registered. */
    fun get(id: ResourceId): T?

    /** Returns the value for [key], or `null` if not registered. */
    fun get(key: ResourceKey<T>): T? {
        ensureSameRegistry(key.registry)
        return get(key.id)
    }

    /** Returns the value for [id], throwing [IllegalArgumentException] if absent. */
    fun require(id: ResourceId): T

    /** Returns the value for [key], throwing [IllegalArgumentException] if absent. */
    fun require(key: ResourceKey<T>): T {
        ensureSameRegistry(key.registry)
        return require(key.id)
    }

    fun contains(id: ResourceId): Boolean

    fun contains(key: ResourceKey<T>): Boolean {
        ensureSameRegistry(key.registry)
        return contains(key.id)
    }

    fun ids(): Collection<ResourceId>

    fun values(): Collection<T>

    fun size(): Int

    fun isFrozen(): Boolean

    private fun ensureSameRegistry(other: RegistryKey<T>) {
        require(key() == other) {
            "Resource key belongs to registry ${other.id}, not ${id()}"
        }
    }
}
