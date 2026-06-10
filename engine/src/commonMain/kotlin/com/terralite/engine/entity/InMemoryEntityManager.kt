package com.terralite.engine.entity

class InMemoryEntityManager : EntityManager {
    private val entities: MutableMap<EntityId, Entity> = LinkedHashMap()
    private var nextId: Long = 1

    override fun create(): Entity {
        val entity = Entity(nextEntityId())
        entities[entity.id] = entity
        return entity
    }

    override fun add(entity: Entity): Entity {
        if (entities.containsKey(entity.id)) {
            throw IllegalArgumentException("Duplicate entity: ${entity.id}")
        }
        entities[entity.id] = entity
        nextId = maxOf(nextId, entity.id.value + 1)
        return entity
    }

    override fun get(id: EntityId): Entity? = entities[id]

    override fun require(id: EntityId): Entity =
        get(id) ?: throw IllegalArgumentException("Missing entity: $id")

    override fun contains(id: EntityId): Boolean = entities.containsKey(id)

    override fun remove(id: EntityId): Entity =
        entities.remove(id) ?: throw IllegalArgumentException("Missing entity: $id")

    override fun ids(): Collection<EntityId> = entities.keys.toList()

    override fun entities(): Collection<Entity> = entities.values.toList()

    override fun size(): Int = entities.size

    private fun nextEntityId(): EntityId {
        while (entities.containsKey(EntityId.of(nextId))) {
            nextId++
        }
        return EntityId.of(nextId++)
    }
}
