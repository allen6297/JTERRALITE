package com.terralite.engine.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryEntityManagerTest {
    @Test
    void createAllocatesMonotonicEntityIds() {
        InMemoryEntityManager entities = new InMemoryEntityManager();

        Entity first = entities.create();
        Entity second = entities.create();

        assertEquals(EntityId.of(1), first.id());
        assertEquals(EntityId.of(2), second.id());
        assertSame(first, entities.require(first.id()));
        assertSame(second, entities.require(second.id()));
    }

    @Test
    void addStoresExplicitEntitiesAndAdvancesNextGeneratedId() {
        InMemoryEntityManager entities = new InMemoryEntityManager();
        Entity explicit = new Entity(EntityId.of(10));

        entities.add(explicit);
        Entity generated = entities.create();

        assertSame(explicit, entities.require(EntityId.of(10)));
        assertEquals(EntityId.of(11), generated.id());
    }

    @Test
    void addRejectsDuplicateEntityIds() {
        InMemoryEntityManager entities = new InMemoryEntityManager();
        Entity first = new Entity(EntityId.of(1));

        entities.add(first);

        assertThrows(IllegalArgumentException.class, () -> entities.add(new Entity(EntityId.of(1))));
    }

    @Test
    void managerPreservesInsertionOrder() {
        InMemoryEntityManager entities = new InMemoryEntityManager();
        Entity first = new Entity(EntityId.of(3));
        Entity second = new Entity(EntityId.of(1));

        entities.add(first);
        entities.add(second);

        assertEquals(List.of(first.id(), second.id()), List.copyOf(entities.ids()));
        assertEquals(List.of(first, second), List.copyOf(entities.entities()));
    }

    @Test
    void removeDeletesExistingEntityAndRejectsMissingEntities() {
        InMemoryEntityManager entities = new InMemoryEntityManager();
        Entity entity = entities.create();

        assertSame(entity, entities.remove(entity.id()));
        assertFalse(entities.contains(entity.id()));
        assertThrows(IllegalArgumentException.class, () -> entities.require(entity.id()));
        assertThrows(IllegalArgumentException.class, () -> entities.remove(entity.id()));
    }

    @Test
    void entityIdsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> EntityId.of(0));
        assertThrows(IllegalArgumentException.class, () -> EntityId.of(-1));
    }

    @Test
    void containsReportsStoredEntities() {
        InMemoryEntityManager entities = new InMemoryEntityManager();
        Entity entity = entities.create();

        assertTrue(entities.contains(entity.id()));
        assertFalse(entities.contains(EntityId.of(99)));
    }
}
