package com.terralite.core.registry

/** Default [MutableRegistry] backed by a linked hash map (insertion order preserved). */
class SimpleMutableRegistry<T>(private val registryKey: RegistryKey<T>) : MutableRegistry<T> {

    private val entries: LinkedHashMap<ResourceId, T> = LinkedHashMap()
    private var frozen = false

    override fun key(): RegistryKey<T> = registryKey

    override fun register(id: ResourceId, value: T): T {
        check(!frozen) { "Registry is frozen: ${registryKey.id}" }
        require(!entries.containsKey(id)) { "Duplicate registry entry: $id" }
        entries[id] = value
        return value
    }

    override fun replace(id: ResourceId, value: T): T {
        check(!frozen) { "Registry is frozen: ${registryKey.id}" }
        require(entries.containsKey(id)) { "No registry entry to replace: $id" }
        entries[id] = value
        return value
    }

    override fun get(id: ResourceId): T? = entries[id]

    override fun require(id: ResourceId): T =
        entries[id] ?: throw IllegalArgumentException("Missing registry entry: $id in ${registryKey.id}")

    override fun contains(id: ResourceId): Boolean = entries.containsKey(id)

    override fun ids(): Collection<ResourceId> = entries.keys.toSet()

    override fun values(): Collection<T> = entries.values.toList()

    override fun size(): Int = entries.size

    override fun isFrozen(): Boolean = frozen

    override fun freeze(): FrozenRegistry<T> {
        frozen = true
        return SimpleFrozenRegistry(registryKey, entries)
    }
}
