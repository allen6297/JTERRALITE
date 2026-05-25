package com.terralite.engine.entity;

import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.player.PlayerComponents;
import com.terralite.engine.world.World;

import java.util.Objects;

public final class EntitySpawner {
    private final World world;

    public EntitySpawner(World world) {
        this.world = Objects.requireNonNull(world, "world");
    }

    public Entity spawn(EntitySpawnRequest request) {
        Objects.requireNonNull(request, "request");
        Entity entity = world.entities().create();

        request.transform().ifPresent(transform -> entity.set(PhysicsComponents.TRANSFORM, transform));
        request.velocity().ifPresent(velocity -> entity.set(PhysicsComponents.VELOCITY, velocity));
        request.playerControlled().ifPresent(player -> entity.set(PlayerComponents.PLAYER_CONTROLLED, player));

        return entity;
    }

    public Entity despawn(EntityId id) {
        return world.entities().remove(Objects.requireNonNull(id, "id"));
    }
}
