package com.terralite.engine.entity

@JvmRecord
data class ComponentType<T>(val name: String) {
    init {
        require(name.isNotBlank()) { "Component type name cannot be blank" }
    }

    companion object {
        @JvmStatic fun <T> of(name: String): ComponentType<T> = ComponentType(name)

        // JVM compat: callers passing Class<T> as second arg — ignored, kept for binary compat
        @JvmStatic fun <T> of(name: String, @Suppress("UNUSED_PARAMETER") type: Class<T>): ComponentType<T> = ComponentType(name)
    }
}
