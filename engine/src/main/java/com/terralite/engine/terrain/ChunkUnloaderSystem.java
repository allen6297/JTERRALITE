package com.terralite.engine.terrain;

import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.util.List;
import java.util.Objects;

public final class ChunkUnloaderSystem implements WorldSimulationSystem {
    private final EntityId target;
    private final int chunkSize;
    private final ChunkLoadRadius radius;

    public ChunkUnloaderSystem(EntityId target, int chunkSize, ChunkLoadRadius radius) {
        this.target = Objects.requireNonNull(target, "target");
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        this.chunkSize = chunkSize;
        this.radius = Objects.requireNonNull(radius, "radius");
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        Entity entity = world.entities().require(target);
        Transform transform = entity.require(PhysicsComponents.TRANSFORM);
        ChunkPos center = chunkPosFor(transform);

        for (ChunkPos pos : List.copyOf(world.chunkPositions())) {
            if (!isWithinRadius(pos, center)) {
                world.removeChunk(pos);
            }
        }
    }

    public boolean isWithinRadius(ChunkPos pos, ChunkPos center) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(center, "center");
        return Math.abs(pos.x() - center.x()) <= radius.horizontal()
            && Math.abs(pos.z() - center.z()) <= radius.horizontal()
            && Math.abs(pos.y() - center.y()) <= radius.vertical();
    }

    public ChunkPos chunkPosFor(Transform transform) {
        Objects.requireNonNull(transform, "transform");
        return ChunkPos.of(
            floorDiv(transform.x(), chunkSize),
            floorDiv(transform.y(), chunkSize),
            floorDiv(transform.z(), chunkSize)
        );
    }

    private static int floorDiv(double value, int divisor) {
        return (int) Math.floor(value / divisor);
    }
}
