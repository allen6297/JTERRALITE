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
import java.util.List;

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
              "sound_type": "stone",
              "model": "terralite:block/cube_all",
              "textures": {
                "all": "terralite:block/stone"
              },
              "state": {
                "properties": {
                  "age": ["0", "1", "2", "3", "4", "5", "6", "7"]
                },
                "default": {
                  "age": "0"
                }
              },
              "states": [
                {
                  "when": { "age": "7" },
                  "model": "terralite:block/wheat_stage7",
                  "textures": {
                    "all": "terralite:block/wheat_stage7"
                  }
                }
              ],
              "categories": ["terralite:building_blocks"]
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
        assertEquals(ResourceId.id("terralite:block/cube_all"), stone.properties().model().id());
        assertEquals(ResourceId.id("terralite:block/stone"), stone.properties().textures().all());
        assertEquals(List.of("0", "1", "2", "3", "4", "5", "6", "7"),
                stone.properties().stateDefinition().properties().get("age"));
        assertEquals("0", stone.properties().stateDefinition().defaultValues().get("age"));
        assertEquals(1, stone.properties().modelVariants().size());
        assertEquals("7", stone.properties().modelVariants().getFirst().when().get("age"));
        assertEquals(ResourceId.id("terralite:block/wheat_stage7"), stone.properties().modelVariants().getFirst().model().id());
        assertEquals(ResourceId.id("terralite:block/wheat_stage7"), stone.properties().modelVariants().getFirst().textures().all());
        assertEquals(List.of(ResourceId.id("terralite:building_blocks")), stone.properties().categories());
    }

    private static ByteArrayInputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
