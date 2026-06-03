package com.terralite.game.block;

import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.game.registry.TerraliteRegistries;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStateRegistryTest {
    @Test
    void generatesStableIdsForEveryDeclaredStateCombination() {
        RegistryManager registries = new RegistryManager();
        var blocks = registries.create(TerraliteRegistries.BLOCKS);
        blocks.register(ResourceId.id("terralite:wheat"), Block.builder()
                .stateDefinition(new BlockStateDefinition(
                        Map.of(
                                "age", List.of("0", "1"),
                                "waterlogged", List.of("false", "true")
                        ),
                        Map.of("age", "0", "waterlogged", "false")
                ))
                .build());
        blocks.register(ResourceId.id("terralite:stone"), Block.builder().build());

        BlockStateRegistry stateRegistry = BlockStateRegistry.from(registries.freeze());

        assertEquals(6, stateRegistry.size());
        assertEquals(BlockState.AIR, stateRegistry.state(0));
        assertEquals(new BlockState(ResourceId.id("terralite:stone")), stateRegistry.state(1));
        assertEquals(new BlockState(ResourceId.id("terralite:wheat"), Map.of("age", "0", "waterlogged", "false")),
                stateRegistry.state(2));
        assertEquals(2, stateRegistry.requireId(new BlockState(
                ResourceId.id("terralite:wheat"),
                Map.of("age", "0", "waterlogged", "false")
        )));
    }

    @Test
    void returnsDefaultStatesForBlocks() {
        ResourceId wheatId = ResourceId.id("terralite:wheat");
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.BLOCKS)
                .register(wheatId, Block.builder()
                        .stateDefinition(new BlockStateDefinition(
                                Map.of("age", List.of("0", "7")),
                                Map.of("age", "0")
                        ))
                        .build());

        BlockStateRegistry stateRegistry = BlockStateRegistry.from(registries.freeze());

        assertEquals(new BlockState(wheatId, Map.of("age", "0")), stateRegistry.defaultState(wheatId));
    }

    @Test
    void createsCompactBlockStorageBackedByStateIds() {
        ResourceId stoneId = ResourceId.id("terralite:stone");
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.BLOCKS)
                .register(stoneId, Block.builder().build());
        BlockStateRegistry stateRegistry = BlockStateRegistry.from(registries.freeze());
        var storage = stateRegistry.createStorage();

        storage.set(BlockPos.of(1, 2, 3), new BlockState(stoneId));

        assertEquals(stateRegistry.requireId(new BlockState(stoneId)), storage.getStateId(BlockPos.of(1, 2, 3)));
        assertEquals(new BlockState(stoneId), storage.get(BlockPos.of(1, 2, 3)));
    }

    @Test
    void rejectsUnknownOrInvalidStates() {
        ResourceId wheatId = ResourceId.id("terralite:wheat");
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.BLOCKS)
                .register(wheatId, Block.builder()
                        .stateDefinition(new BlockStateDefinition(
                                Map.of("age", List.of("0", "7")),
                                Map.of("age", "0")
                        ))
                        .build());
        BlockStateRegistry stateRegistry = BlockStateRegistry.from(registries.freeze());

        assertTrue(stateRegistry.id(new BlockState(wheatId, Map.of("age", "banana"))).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> stateRegistry.state(wheatId, Map.of("age", "banana")));
    }
}
