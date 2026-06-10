package com.terralite.engine.entity

class Entity(@get:JvmName("id") val id: EntityId) {
    private val components: MutableMap<ComponentType<*>, Any> = LinkedHashMap()

    fun <T : Any> set(type: ComponentType<T>, component: T): Entity {
        components[type] = component
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: ComponentType<T>): T? = components[type] as T?

    fun <T : Any> require(type: ComponentType<T>): T =
        get(type) ?: throw IllegalArgumentException("Missing component: ${type.name}")

    fun has(type: ComponentType<*>): Boolean = components.containsKey(type)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> remove(type: ComponentType<T>): T? = components.remove(type) as T?

    fun componentCount(): Int = components.size
}
