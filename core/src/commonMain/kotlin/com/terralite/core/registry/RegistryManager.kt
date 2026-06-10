package com.terralite.core.registry

/**
 * Accumulates [MutableRegistry] instances and seals them all into a [GameData] snapshot.
 *
 * Registries must be created before [freeze] is called; afterwards no new registries
 * or entries can be added.
 */
class RegistryManager {

    private val registries: LinkedHashMap<RegistryKey<*>, MutableRegistry<*>> = LinkedHashMap()
    private var frozen = false

    fun <T> create(key: RegistryKey<T>): MutableRegistry<T> {
        check(!frozen) { "Registry manager is frozen" }
        require(!registries.containsKey(key)) { "Duplicate registry: ${key.id}" }
        val registry = SimpleMutableRegistry(key)
        registries[key] = registry
        return registry
    }

    fun <T> requireMutable(key: RegistryKey<T>): MutableRegistry<T> {
        val registry = registries[key]
            ?: throw IllegalArgumentException("Missing registry: ${key.id}")
        return cast(key, registry)
    }

    fun freeze(): GameData {
        frozen = true
        val frozen = LinkedHashMap<RegistryKey<*>, FrozenRegistry<*>>()
        for ((key, registry) in registries) {
            frozen[key] = registry.freeze()
        }
        return GameData(frozen)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> cast(key: RegistryKey<T>, registry: MutableRegistry<*>): MutableRegistry<T> {
        return registry as MutableRegistry<T>
    }
}
