package com.terralite.engine.save;

import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockStorage;
import com.terralite.engine.world.World;
import com.terralite.engine.entity.EntityId;

import java.util.List;
import java.util.Objects;

public final class WorldSnapshotter {
    public WorldSnapshot snapshot(World world) {
        Objects.requireNonNull(world, "world");

        List<EntitySnapshot> entities = world.entities().entities().stream()
            .map(Entity::id)
            .map(EntitySnapshot::new)
            .toList();

        List<BlockSnapshot> blocks = world.blocks().positions().stream()
            .map(pos -> snapshotBlock(world, pos))
            .toList();

        return new WorldSnapshot(List.copyOf(world.chunkPositions()), entities, blocks);
    }

    public World restore(WorldSnapshot snapshot) {
        return restore(snapshot, null);
    }

    public World restore(WorldSnapshot snapshot, BlockStorage blockStorage) {
        Objects.requireNonNull(snapshot, "snapshot");

        World world = blockStorage == null ? new World() : new World(blockStorage);
        for (var chunkPos : snapshot.chunks()) {
            world.putChunk(new Chunk(chunkPos));
        }
        for (BlockSnapshot block : snapshot.blocks()) {
            world.setBlock(block.pos(), block.state());
        }
        for (EntitySnapshot entity : snapshot.entities()) {
            world.entities().add(new Entity(EntityId.of(entity.id().value())));
        }
        return world;
    }

    private static BlockSnapshot snapshotBlock(World world, BlockPos pos) {
        Integer stateId = world.blocks().stateId(pos).isPresent()
            ? world.blocks().stateId(pos).getAsInt()
            : null;
        return new BlockSnapshot(pos, world.getBlock(pos), stateId);
    }
}
