package com.terralite.core.registry

/**
 * Immutable snapshot of all game registries, produced by [RegistryManager.freeze].
 *
 * Obtain a specific registry with [registry]; retrieve an entry directly with [get].
 */
class GameData internal constructor(
    private val registries: Map<RegistryKey<*>, FrozenRegistry<*>>
) {
    fun <T> registry(key: RegistryKey<T>): FrozenRegistry<T> {
        val registry = registries[key]
            ?: throw IllegalArgumentException("Missing registry: ${key.id}")
        return cast(key, registry)
    }

    fun <T> get(key: ResourceKey<T>): T = registry(key.registry).require(key)

    @Suppress("UNCHECKED_CAST")
    private fun <T> cast(key: RegistryKey<T>, registry: FrozenRegistry<*>): FrozenRegistry<T> {
        return registry as FrozenRegistry<T>
    }
}
