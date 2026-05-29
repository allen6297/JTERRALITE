package com.terralite.game.content;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.core.registry.ResourceId;
import com.terralite.core.registry.ResourceKey;
import com.terralite.game.block.Block;
import com.terralite.game.item.Item;
import com.terralite.game.registry.TerraliteRegistries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameContentLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsPacksInDependencyOrderAndReturnsFrozenGameData() throws Exception {
        ContentPack addon = writeAddonPack();
        ContentPack base = writeBasePack();

        GameContentLoadReport report = new GameContentLoader().load(List.of(addon, base));

        Block stone = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "terralite:stone"));
        Item pickaxe = report.gameData().get(ResourceKey.of(TerraliteRegistries.ITEMS, "terralite:iron_pickaxe"));

        assertEquals(List.of(
                ResourceId.id("terralite:base"),
                ResourceId.id("terralite:addon")
        ), report.packs().stream().map(pack -> pack.manifest().id()).toList());
        assertEquals(3, report.loadResult().loadedFiles());
        assertEquals(1.5f, stone.properties().hardness());
        assertEquals(List.of(ResourceId.id("terralite:building_blocks")), pickaxe.properties().categories());
    }

    private ContentPack writeBasePack() throws Exception {
        Path packRoot = tempDir.resolve("base");
        Files.createDirectories(packRoot.resolve("data/terralite/blocks"));
        Files.createDirectories(packRoot.resolve("data/terralite/creative_categories"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:base",
              "name": "Base",
              "version": "1.0.0"
            }
            """);
        Files.writeString(packRoot.resolve("data/terralite/blocks/stone.json"), """
            {
              "hardness": 1.5,
              "resistance": 6.0,
              "categories": ["terralite:building_blocks"]
            }
            """);
        Files.writeString(packRoot.resolve("data/terralite/creative_categories/building_blocks.json"), """
            {
              "title": "Building Blocks",
              "icon": "terralite:stone",
              "entries": ["terralite:stone", "terralite:iron_pickaxe"]
            }
            """);
        return new ContentPackLoader().load(packRoot);
    }

    private ContentPack writeAddonPack() throws Exception {
        Path packRoot = tempDir.resolve("addon");
        Files.createDirectories(packRoot.resolve("data/terralite/items"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:addon",
              "name": "Addon",
              "version": "1.0.0",
              "dependencies": [{ "id": "terralite:base" }]
            }
            """);
        Files.writeString(packRoot.resolve("data/terralite/items/iron_pickaxe.json"), """
            {
              "weight": 3.0,
              "categories": ["terralite:building_blocks"]
            }
            """);
        return new ContentPackLoader().load(packRoot);
    }
}
