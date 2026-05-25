package com.terralite.engine.entity;

import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityComponentTest {
    @Test
    void entityStoresAndRequiresComponentsByType() {
        Entity entity = new Entity(EntityId.of(1));
        Transform transform = new Transform(1.0, 2.0, 3.0);

        assertSame(entity, entity.set(PhysicsComponents.TRANSFORM, transform));

        assertTrue(entity.has(PhysicsComponents.TRANSFORM));
        assertEquals(transform, entity.require(PhysicsComponents.TRANSFORM));
        assertEquals(transform, entity.get(PhysicsComponents.TRANSFORM).orElseThrow());
        assertEquals(1, entity.componentCount());
    }

    @Test
    void entityReplacesComponentsOfSameType() {
        Entity entity = new Entity(EntityId.of(1));

        entity.set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);
        entity.set(PhysicsComponents.TRANSFORM, new Transform(5.0, 0.0, 0.0));

        assertEquals(new Transform(5.0, 0.0, 0.0), entity.require(PhysicsComponents.TRANSFORM));
        assertEquals(1, entity.componentCount());
    }

    @Test
    void entityRemovesComponents() {
        Entity entity = new Entity(EntityId.of(1));
        Transform transform = Transform.ORIGIN;

        entity.set(PhysicsComponents.TRANSFORM, transform);

        assertEquals(transform, entity.remove(PhysicsComponents.TRANSFORM).orElseThrow());
        assertFalse(entity.has(PhysicsComponents.TRANSFORM));
        assertThrows(IllegalArgumentException.class, () -> entity.require(PhysicsComponents.TRANSFORM));
    }

    @Test
    void componentTypeRequiresAName() {
        assertThrows(IllegalArgumentException.class, () -> ComponentType.of(" ", Transform.class));
    }
}
