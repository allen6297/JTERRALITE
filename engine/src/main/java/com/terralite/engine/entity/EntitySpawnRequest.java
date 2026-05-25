package com.terralite.engine.entity;

import com.terralite.engine.physics.Transform;
import com.terralite.engine.physics.Velocity;
import com.terralite.engine.player.PlayerControlled;

import java.util.Objects;
import java.util.Optional;

public final class EntitySpawnRequest {
    private Transform transform;
    private Velocity velocity;
    private PlayerControlled playerControlled;

    public static EntitySpawnRequest create() {
        return new EntitySpawnRequest();
    }

    public EntitySpawnRequest transform(Transform transform) {
        this.transform = Objects.requireNonNull(transform, "transform");
        return this;
    }

    public EntitySpawnRequest velocity(Velocity velocity) {
        this.velocity = Objects.requireNonNull(velocity, "velocity");
        return this;
    }

    public EntitySpawnRequest playerControlled(PlayerControlled playerControlled) {
        this.playerControlled = Objects.requireNonNull(playerControlled, "playerControlled");
        return this;
    }

    public Optional<Transform> transform() {
        return Optional.ofNullable(transform);
    }

    public Optional<Velocity> velocity() {
        return Optional.ofNullable(velocity);
    }

    public Optional<PlayerControlled> playerControlled() {
        return Optional.ofNullable(playerControlled);
    }
}
