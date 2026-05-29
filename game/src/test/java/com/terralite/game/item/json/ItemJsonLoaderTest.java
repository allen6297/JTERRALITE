package com.terralite.game.item.json;

import com.terralite.core.registry.GameData;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.core.registry.ResourceKey;
import com.terralite.game.item.Item;
import com.terralite.game.registry.TerraliteRegistries;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemJsonLoaderTest {
    private static final ResourceKey<Item> IRON_PICKAXE =
            ResourceKey.of(TerraliteRegistries.ITEMS, "terralite:iron_pickaxe");

    @Test
    void loadsItemJsonIntoRegistry() throws Exception {
        String json = """
            {
              "weight": 3.0,
              "categories": ["terralite:tools_and_utilities"]
            }
            """;

        RegistryManager registries = new RegistryManager();
        MutableRegistry<Item> items = registries.create(TerraliteRegistries.ITEMS);

        new ItemJsonLoader().register(ResourceId.id("terralite:iron_pickaxe"), stream(json), items);

        GameData gameData = registries.freeze();
        Item pickaxe = gameData.get(IRON_PICKAXE);

        assertEquals(3.0f, pickaxe.properties().weight());
        assertEquals(List.of(ResourceId.id("terralite:tools_and_utilities")), pickaxe.properties().categories());
    }

    private static ByteArrayInputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
