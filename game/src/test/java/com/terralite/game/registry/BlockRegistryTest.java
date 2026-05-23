package com.terralite.game.registry;

import com.terralite.core.registry.GameData;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.core.registry.ResourceKey;
import com.terralite.game.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BlockRegistryTest {
    private static final ResourceKey<Block> STONE = ResourceKey.of(TerraliteRegistries.BLOCKS, "terralite:stone");

    @Test
    void registersAndReadsBlockFromFrozenGameData() {
        RegistryManager registries = new RegistryManager();
        MutableRegistry<Block> blocks = registries.create(TerraliteRegistries.BLOCKS);

        Block stone = Block.builder()
            .hardness(1.5f)
            .resistance(6.0f)
            .requiresTool(true)
            .material("stone")
            .soundType("stone")
            .build();

        blocks.register(STONE, stone);

        GameData gameData = registries.freeze();
        Block registeredStone = gameData.get(STONE);

        assertSame(stone, registeredStone);
        assertEquals(1.5f, registeredStone.properties().hardness());
        assertEquals(6.0f, registeredStone.properties().resistance());
        assertEquals(ResourceId.id("terralite:stone"), gameData.registry(TerraliteRegistries.BLOCKS).ids().iterator().next());
    }
}
