package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CollisionDetector {
    public List<CollisionPair> detect(World world) {
        Objects.requireNonNull(world, "world");
        List<Entity> collidable = world.entities().entities().stream()
            .filter(entity -> entity.has(PhysicsComponents.TRANSFORM))
            .filter(entity -> entity.has(PhysicsComponents.COLLIDER))
            .toList();
        List<CollisionPair> collisions = new ArrayList<>();

        for (int i = 0; i < collidable.size(); i++) {
            Entity first = collidable.get(i);
            Aabb firstBounds = bounds(first);
            for (int j = i + 1; j < collidable.size(); j++) {
                Entity second = collidable.get(j);
                if (firstBounds.intersects(bounds(second))) {
                    collisions.add(new CollisionPair(first, second));
                }
            }
        }

        return List.copyOf(collisions);
    }

    private static Aabb bounds(Entity entity) {
        Transform transform = entity.require(PhysicsComponents.TRANSFORM);
        Collider collider = entity.require(PhysicsComponents.COLLIDER);
        return collider.boundsAt(transform);
    }
}
