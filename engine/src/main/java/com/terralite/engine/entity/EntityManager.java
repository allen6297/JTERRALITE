package com.terralite.engine.entity;

import java.util.Collection;
import java.util.Optional;

public interface EntityManager {
    Entity create();

    Entity add(Entity entity);

    Optional<Entity> get(EntityId id);

    Entity require(EntityId id);

    boolean contains(EntityId id);

    Entity remove(EntityId id);

    Collection<EntityId> ids();

    Collection<Entity> entities();

    int size();
}
