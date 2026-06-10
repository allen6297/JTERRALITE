package com.terralite.core.registry

/**
 * Typed key that identifies a registry.
 *
 * The type parameter `T` provides compile-time safety when obtaining a registry
 * from [GameData]. The runtime class is intentionally omitted — the generic
 * bound alone is sufficient for correctness, and it avoids the JVM/Android
 * split between `Class<T>` and `KClass<T>`.
 */
@JvmRecord
data class RegistryKey<T>(val id: ResourceId) {

    companion object {
        @JvmStatic
        fun <T> of(id: ResourceId): RegistryKey<T> = RegistryKey(id)

        @JvmStatic
        fun <T> of(id: String): RegistryKey<T> = RegistryKey(ResourceId.parse(id))
    }
}
