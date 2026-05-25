package com.terralite.engine.entity;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryEntityManager implements EntityManager {
    private final Map<EntityId, Entity> entities = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Entity create() {
        Entity entity = new Entity(nextEntityId());
        entities.put(entity.id(), entity);
        return entity;
    }

    @Override
    public Entity add(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (entities.containsKey(entity.id())) {
            throw new IllegalArgumentException("Duplicate entity: " + entity.id());
        }

        entities.put(entity.id(), entity);
        nextId = Math.max(nextId, entity.id().value() + 1);
        return entity;
    }

    @Override
    public Optional<Entity> get(EntityId id) {
        return Optional.ofNullable(entities.get(Objects.requireNonNull(id, "id")));
    }

    @Override
    public Entity require(EntityId id) {
        return get(id).orElseThrow(() -> new IllegalArgumentException("Missing entity: " + id));
    }

    @Override
    public boolean contains(EntityId id) {
        return entities.containsKey(Objects.requireNonNull(id, "id"));
    }

    @Override
    public Entity remove(EntityId id) {
        Entity removed = entities.remove(Objects.requireNonNull(id, "id"));
        if (removed == null) {
            throw new IllegalArgumentException("Missing entity: " + id);
        }
        return removed;
    }

    @Override
    public Collection<EntityId> ids() {
        return List.copyOf(entities.keySet());
    }

    @Override
    public Collection<Entity> entities() {
        return List.copyOf(entities.values());
    }

    @Override
    public int size() {
        return entities.size();
    }

    private EntityId nextEntityId() {
        while (entities.containsKey(EntityId.of(nextId))) {
            nextId++;
        }
        return EntityId.of(nextId++);
    }
}
