package com.terralite.game.content;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.core.registry.ResourceId;
import com.terralite.core.registry.ResourceKey;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.game.tag.Tag;
import com.terralite.game.worldsgen.WorldsgenSpawnArea;
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
        assertEquals(1, report.startupScripts().executedScripts());
        assertEquals("addon startup", report.startupScripts().messages().get(0).message());
        assertEquals(1.5f, stone.properties().hardness());
        assertEquals(List.of(ResourceId.id("terralite:building_blocks")), pickaxe.properties().categories());
    }

    @Test
    void loadsRepoGameContentDirectories() throws Exception {
        GameContentLoadReport report = new GameContentLoader().load(repoPacksRoot());

        Block stone = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "terralite:natural/stone"));
        Block grassBlock = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "terralite:natural/grass_block"));
        Item wheatSeeds = report.gameData().get(ResourceKey.of(TerraliteRegistries.ITEMS, "terralite:seeds/wheat_seeds"));
        Item stoneProbe = report.gameData().get(ResourceKey.of(TerraliteRegistries.ITEMS, "terralite:tools/stone_probe"));
        Biome plains = report.gameData().get(ResourceKey.of(TerraliteRegistries.BIOMES, "terralite:plains"));
        Tag naturalBlocks = report.gameData().get(ResourceKey.of(TerraliteRegistries.TAGS, "terralite:natural_blocks"));
        Tag scriptedItems = report.gameData().get(ResourceKey.of(TerraliteRegistries.TAGS, "terralite:scripted_items"));
        WorldsgenSpawnArea spawnArea =
                report.gameData().get(ResourceKey.of(TerraliteRegistries.WORLDSGEN_SPAWN_AREAS, "terralite:spawn_area"));
        CreativeCategory naturalItems =
                report.gameData().get(ResourceKey.of(TerraliteRegistries.CREATIVE_CATEGORIES, "terralite:natural_items"));
        CreativeCategory scriptedTools =
                report.gameData().get(ResourceKey.of(TerraliteRegistries.CREATIVE_CATEGORIES, "terralite:scripted_tools"));

        assertEquals(List.of(ResourceId.id("terralite:base")), report.packs().stream().map(pack -> pack.manifest().id()).toList());
        assertEquals(17, report.loadResult().scannedFiles());
        assertEquals(13, report.loadResult().loadedFiles());
        assertEquals(4, report.loadResult().skippedFiles().size());
        assertEquals(1, report.startupScripts().executedScripts());
        assertEquals("Terralite base startup script loaded", report.startupScripts().messages().get(0).message());
        assertEquals("Stone", stone.properties().displayName());
        assertEquals(6.5f, stone.properties().resistance());
        assertEquals(ResourceId.id("terralite:block/cube_column"), grassBlock.properties().model().id());
        assertEquals(ResourceId.id("terralite:block/grass_block_top"), grassBlock.properties().textures().top());
        assertEquals("terralite:crops/wheat", wheatSeeds.properties().placesBlock());
        assertEquals("Stone Probe", stoneProbe.properties().displayName());
        assertEquals(List.of(ResourceId.id("terralite:scripted_tools")), stoneProbe.properties().categories());
        assertEquals("Plains", plains.properties().name());
        assertEquals(List.of(
                ResourceId.id("terralite:natural/stone"),
                ResourceId.id("terralite:natural/dirt"),
                ResourceId.id("terralite:natural/grass_block")
        ), naturalBlocks.members());
        assertEquals(List.of(ResourceId.id("terralite:tools/stone_probe")), scriptedItems.members());
        assertEquals(ResourceId.id("terralite:materials/stone_shard"), naturalItems.icon());
        assertEquals("Scripted Tools", scriptedTools.title());
        assertEquals(ResourceId.id("terralite:tools/stone_probe"), scriptedTools.icon());
        assertEquals(List.of(ResourceId.id("terralite:tools/stone_probe")), scriptedTools.entries());
        assertEquals(9, spawnArea.chunkPositions().size());
    }

    private ContentPack writeBasePack() throws Exception {
        Path packRoot = tempDir.resolve("base");
        Files.createDirectories(packRoot.resolve("data/blocks"));
        Files.createDirectories(packRoot.resolve("data/creative_categories"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:base",
              "name": "Base",
              "version": "1.0.0"
            }
            """);
        Files.writeString(packRoot.resolve("data/blocks/stone.json"), """
            {
              "hardness": 1.5,
              "resistance": 6.0,
              "categories": ["terralite:building_blocks"]
            }
            """);
        Files.writeString(packRoot.resolve("data/creative_categories/building_blocks.json"), """
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
        Files.createDirectories(packRoot.resolve("data/items"));
        Files.createDirectories(packRoot.resolve("scripts/startup"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:addon",
              "name": "Addon",
              "version": "1.0.0",
              "dependencies": [{ "id": "terralite:base" }]
            }
            """);
        Files.writeString(packRoot.resolve("data/items/iron_pickaxe.json"), """
            {
              "weight": 3.0,
              "categories": ["terralite:building_blocks"]
            }
            """);
        Files.writeString(packRoot.resolve("scripts/startup/main.js"), "api.info('addon startup');");
        return new ContentPackLoader().load(packRoot);
    }

    private static Path repoPacksRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = workingDirectory.resolve("packs");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return workingDirectory.resolve("../packs").normalize();
    }
}
