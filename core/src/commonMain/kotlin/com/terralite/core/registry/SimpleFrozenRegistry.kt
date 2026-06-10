package com.terralite.core.registry

/** Immutable snapshot created by [SimpleMutableRegistry.freeze]. */
class SimpleFrozenRegistry<T>(
    private val registryKey: RegistryKey<T>,
    entries: Map<ResourceId, T>
) : FrozenRegistry<T> {

    private val entries: Map<ResourceId, T> = LinkedHashMap(entries)

    override fun key(): RegistryKey<T> = registryKey

    override fun get(id: ResourceId): T? = entries[id]

    override fun require(id: ResourceId): T =
        entries[id] ?: throw IllegalArgumentException("Missing registry entry: $id in ${registryKey.id}")

    override fun contains(id: ResourceId): Boolean = entries.containsKey(id)

    override fun ids(): Collection<ResourceId> = entries.keys

    override fun values(): Collection<T> = entries.values

    override fun size(): Int = entries.size
}
