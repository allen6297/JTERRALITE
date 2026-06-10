package com.terralite.core.registry

/** A fully-qualified key that combines the target [registry] with a specific entry [id]. */
@JvmRecord
data class ResourceKey<T>(val registry: RegistryKey<T>, val id: ResourceId) {

    companion object {
        @JvmStatic
        fun <T> of(registry: RegistryKey<T>, id: ResourceId): ResourceKey<T> = ResourceKey(registry, id)

        @JvmStatic
        fun <T> of(registry: RegistryKey<T>, id: String): ResourceKey<T> = ResourceKey(registry, ResourceId.parse(id))
    }
}
