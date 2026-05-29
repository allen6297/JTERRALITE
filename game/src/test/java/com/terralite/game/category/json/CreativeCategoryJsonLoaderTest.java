package com.terralite.game.category.json;

import com.terralite.core.registry.GameData;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.core.registry.ResourceKey;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.registry.TerraliteRegistries;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreativeCategoryJsonLoaderTest {
    private static final ResourceKey<CreativeCategory> BUILDING_BLOCKS =
            ResourceKey.of(TerraliteRegistries.CREATIVE_CATEGORIES, "terralite:building_blocks");

    @Test
    void loadsCreativeCategoryJsonIntoRegistry() throws Exception {
        String json = """
            {
              "title": "Building Blocks",
              "icon": "terralite:stone",
              "entries": ["terralite:stone", "terralite:dirt"]
            }
            """;

        RegistryManager registries = new RegistryManager();
        MutableRegistry<CreativeCategory> categories = registries.create(TerraliteRegistries.CREATIVE_CATEGORIES);

        new CreativeCategoryJsonLoader().register(ResourceId.id("terralite:building_blocks"), stream(json), categories);

        GameData gameData = registries.freeze();
        CreativeCategory category = gameData.get(BUILDING_BLOCKS);

        assertEquals("Building Blocks", category.title());
        assertEquals(ResourceId.id("terralite:stone"), category.icon());
        assertEquals(List.of(ResourceId.id("terralite:stone"), ResourceId.id("terralite:dirt")), category.entries());
    }

    private static ByteArrayInputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
