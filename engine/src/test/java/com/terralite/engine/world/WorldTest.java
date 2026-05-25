package com.terralite.engine.world;

import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.entity.EntitySpawnRequest;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldTest {
    @Test
    void worldStoresAndExposesChunksThroughWorldAccess() {
        World world = new World();
        WorldAccess access = world;
        Chunk chunk = new Chunk(ChunkPos.of(4, 0, 7));

        world.putChunk(chunk);

        assertTrue(access.containsChunk(chunk.pos()));
        assertSame(chunk, access.requireChunk(chunk.pos()));
        assertEquals(chunk, access.getChunk(chunk.pos()).orElseThrow());
        assertEquals(List.of(chunk.pos()), List.copyOf(access.chunkPositions()));
    }

    @Test
    void worldRemovesChunks() {
        World world = new World();
        Chunk chunk = world.putChunk(new Chunk(ChunkPos.of(1, 0, 1)));

        assertSame(chunk, world.removeChunk(chunk.pos()));
        assertTrue(world.chunkPositions().isEmpty());
    }

    @Test
    void worldOwnsAnEntityManager() {
        World world = new World();
        Entity entity = world.entities().create();

        assertEquals(EntityId.of(1), entity.id());
        assertSame(entity, world.entities().require(entity.id()));
    }

    @Test
    void worldCreatesSpawnerForItsEntityManager() {
        World world = new World();
        Entity entity = world.spawner().spawn(EntitySpawnRequest.create());

        assertSame(entity, world.entities().require(entity.id()));
    }

    @Test
    void worldStoresSparseBlocks() {
        World world = new World();
        BlockPos pos = BlockPos.of(1, 2, 3);
        BlockState stone = BlockState.of("terralite:stone");

        assertEquals(BlockState.AIR, world.getBlock(pos));

        world.setBlock(pos, stone);

        assertEquals(stone, world.getBlock(pos));
        assertEquals(1, world.blocks().size());
    }
}
