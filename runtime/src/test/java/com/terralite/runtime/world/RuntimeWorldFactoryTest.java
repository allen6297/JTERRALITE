package com.terralite.runtime.world;

import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.game.worldsgen.WorldsgenSpawnArea;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeWorldFactoryTest {
    @Test
    void createsWorldChunksFromSpawnAreaRegistry() {
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.WORLDSGEN_SPAWN_AREAS)
                .register(ResourceId.id("example:spawn_area"), WorldsgenSpawnArea.builder()
                        .center(2, 1, -3)
                        .radius(0, 1)
                        .build());

        var world = new RuntimeWorldFactory().create(
                registries.freeze(),
                ResourceId.id("example:spawn_area")
        );

        assertEquals(3, world.chunkPositions().size());
        assertTrue(world.containsChunk(ChunkPos.of(2, 0, -3)));
        assertTrue(world.containsChunk(ChunkPos.of(2, 1, -3)));
        assertTrue(world.containsChunk(ChunkPos.of(2, 2, -3)));
        assertEquals(BlockState.of("terralite:natural/grass_block"), world.getBlock(BlockPos.of(32, 0, -48)));
    }

    @Test
    void fallsBackToDefaultSpawnAreaWhenMissing() {
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.WORLDSGEN_SPAWN_AREAS);

        var world = new RuntimeWorldFactory().create(registries.freeze(), ResourceId.id("example:missing"));

        assertEquals(9, world.chunkPositions().size());
    }
}
