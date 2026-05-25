package com.terralite.engine.save;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.world.World;

import java.util.List;
import java.util.Objects;

public final class WorldSnapshotter {
    public WorldSnapshot snapshot(World world) {
        Objects.requireNonNull(world, "world");

        List<EntitySnapshot> entities = world.entities().entities().stream()
            .map(Entity::id)
            .map(EntitySnapshot::new)
            .toList();

        return new WorldSnapshot(List.copyOf(world.chunkPositions()), entities);
    }
}
