package com.terralite.engine.entity

interface EntityManager {
    fun create(): Entity
    fun add(entity: Entity): Entity
    fun get(id: EntityId): Entity?
    fun require(id: EntityId): Entity
    fun contains(id: EntityId): Boolean
    fun remove(id: EntityId): Entity
    fun ids(): Collection<EntityId>
    fun entities(): Collection<Entity>
    fun size(): Int
}
