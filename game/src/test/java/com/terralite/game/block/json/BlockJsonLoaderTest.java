package com.terralite.game.block.json;

import com.terralite.core.registry.GameData;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.core.registry.ResourceKey;
import com.terralite.game.block.Block;
import com.terralite.game.registry.TerraliteRegistries;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockJsonLoaderTest {
    private static final ResourceKey<Block> STONE = ResourceKey.of(TerraliteRegistries.BLOCKS, "terralite:stone");

    @Test
    void loadsBlockJsonIntoRegistry() throws Exception {
        String json = """
            {
              "hardness": 1.5,
              "resistance": 6.0,
              "solid": true,
              "transparent": false,
              "requires_tool": true,
              "material": "stone",
              "sound_type": "stone"
            }
            """;

        RegistryManager registries = new RegistryManager();
        MutableRegistry<Block> blocks = registries.create(TerraliteRegistries.BLOCKS);

        new BlockJsonLoader().register(ResourceId.id("terralite:stone"), stream(json), blocks);

        GameData gameData = registries.freeze();
        Block stone = gameData.get(STONE);

        assertEquals(1.5f, stone.properties().hardness());
        assertEquals(6.0f, stone.properties().resistance());
        assertTrue(stone.properties().requiresTool());
        assertEquals("stone", stone.properties().material());
        assertEquals("stone", stone.properties().soundType());
    }

    private static ByteArrayInputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
