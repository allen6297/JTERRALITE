package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollisionDetectorTest {
    @Test
    void detectorFindsOverlappingCollidableEntitiesInDeterministicOrder() {
        World world = new World();
        Entity first = collidable(world, new Transform(0.0, 0.0, 0.0));
        Entity second = collidable(world, new Transform(1.0, 0.0, 0.0));
        collidable(world, new Transform(5.0, 0.0, 0.0));

        List<CollisionPair> collisions = new CollisionDetector().detect(world);

        assertEquals(1, collisions.size());
        assertSame(first, collisions.getFirst().first());
        assertSame(second, collisions.getFirst().second());
    }

    @Test
    void detectorIgnoresEntitiesMissingTransformOrCollider() {
        World world = new World();
        world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);
        world.entities().create()
            .set(PhysicsComponents.COLLIDER, new Collider(1.0, 1.0, 1.0));

        assertTrue(new CollisionDetector().detect(world).isEmpty());
    }

    @Test
    void collisionPairRequiresDistinctEntities() {
        Entity entity = new Entity(EntityId.of(1));

        assertThrows(IllegalArgumentException.class, () -> new CollisionPair(entity, entity));
        assertThrows(IllegalArgumentException.class, () -> new CollisionPair(entity, new Entity(EntityId.of(1))));
    }

    private static Entity collidable(World world, Transform transform) {
        return world.entities().create()
            .set(PhysicsComponents.TRANSFORM, transform)
            .set(PhysicsComponents.COLLIDER, new Collider(1.0, 1.0, 1.0));
    }
}
