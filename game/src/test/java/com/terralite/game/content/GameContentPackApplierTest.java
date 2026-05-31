package com.terralite.game.content;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.core.registry.GameData;
import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.core.registry.ResourceKey;
import com.terralite.game.block.Block;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.registry.TerraliteRegistries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameContentPackApplierTest {
    @TempDir
    Path tempDir;

    @Test
    void appliesGameJsonFilesIntoRegistries() throws Exception {
        Path packRoot = tempDir.resolve("base");
        writePack(packRoot);

        ContentPack pack = new ContentPackLoader().load(packRoot);
        RegistryManager registries = new RegistryManager();

        GameContentLoadResult result = new GameContentPackApplier().apply(pack, registries);
        GameData gameData = registries.freeze();

        Block stone = gameData.get(ResourceKey.of(TerraliteRegistries.BLOCKS, "terralite:natural/stone"));
        Item pickaxe = gameData.get(ResourceKey.of(TerraliteRegistries.ITEMS, "terralite:iron_pickaxe"));
        CreativeCategory buildingBlocks =
                gameData.get(ResourceKey.of(TerraliteRegistries.CREATIVE_CATEGORIES, "terralite:building_blocks"));

        assertEquals(5, result.scannedFiles());
        assertEquals(3, result.loadedFiles());
        assertEquals(2, result.skippedFiles().size());
        assertEquals(1.5f, stone.properties().hardness());
        assertEquals(List.of(ResourceId.id("terralite:building_blocks")), stone.properties().categories());
        assertEquals(3.0f, pickaxe.properties().weight());
        assertEquals("Building Blocks", buildingBlocks.title());
        assertEquals(List.of(ResourceId.id("terralite:natural/stone")), buildingBlocks.entries());
    }

    private static void writePack(Path packRoot) throws Exception {
        Files.createDirectories(packRoot.resolve("data/blocks/natural"));
        Files.createDirectories(packRoot.resolve("data/items"));
        Files.createDirectories(packRoot.resolve("data/creative_categories"));
        Files.createDirectories(packRoot.resolve("data/recipes"));
        Files.createDirectories(packRoot.resolve("assets/lang"));

        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:base",
              "name": "Terralite Base",
              "version": "1.0.0"
            }
            """);
        Files.writeString(packRoot.resolve("data/blocks/natural/stone.json"), """
            {
              "hardness": 1.5,
              "resistance": 6.0,
              "solid": true,
              "material": "stone",
              "sound_type": "stone",
              "categories": ["terralite:building_blocks"]
            }
            """);
        Files.writeString(packRoot.resolve("data/items/iron_pickaxe.json"), """
            {
              "weight": 3.0,
              "categories": ["terralite:tools_and_utilities"]
            }
            """);
        Files.writeString(packRoot.resolve("data/creative_categories/building_blocks.json"), """
            {
              "title": "Building Blocks",
              "icon": "terralite:natural/stone",
              "entries": ["terralite:natural/stone"]
            }
            """);
        Files.writeString(packRoot.resolve("data/recipes/stonecutting.json"), "{}");
        Files.writeString(packRoot.resolve("assets/lang/en_us.json"), "{}");
    }
}
