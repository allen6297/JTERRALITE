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
        Block doubleStone = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "terralite:natural/double_stone"));
        Block wheat = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "terralite:crops/wheat"));
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
        assertEquals(22, report.loadResult().scannedFiles());
        assertEquals(14, report.loadResult().loadedFiles());
        assertEquals(8, report.loadResult().skippedFiles().size());
        assertEquals(1, report.startupScripts().executedScripts());
        assertEquals("Terralite base startup script loaded", report.startupScripts().messages().get(0).message());
        assertEquals("Stone", stone.properties().displayName());
        assertEquals(6.5f, stone.properties().resistance());
        assertEquals(ResourceId.id("terralite:block/cube_column"), grassBlock.properties().model().id());
        assertEquals(ResourceId.id("terralite:block/grass_block_top"), grassBlock.properties().textures().top());
        assertEquals(ResourceId.id("terralite:block/double_stone"), doubleStone.properties().model().id());
        assertEquals(3, wheat.properties().modelVariants().size());
        assertEquals(ResourceId.id("terralite:block/wheat_stage7"), wheat.properties().modelVariants().get(2).model().id());
        assertEquals("terralite:crops/wheat", wheatSeeds.properties().placesBlock());
        assertEquals("Stone Probe", stoneProbe.properties().displayName());
        assertEquals(List.of(ResourceId.id("terralite:scripted_tools")), stoneProbe.properties().categories());
        assertEquals("Plains", plains.properties().name());
        assertEquals(List.of(
                ResourceId.id("terralite:natural/stone"),
                ResourceId.id("terralite:natural/dirt"),
                ResourceId.id("terralite:natural/grass_block"),
                ResourceId.id("terralite:natural/double_stone")
        ), naturalBlocks.members());
        assertEquals(List.of(ResourceId.id("terralite:tools/stone_probe")), scriptedItems.members());
        assertEquals(ResourceId.id("terralite:materials/stone_shard"), naturalItems.icon());
        assertEquals("Scripted Tools", scriptedTools.title());
        assertEquals(ResourceId.id("terralite:tools/stone_probe"), scriptedTools.icon());
        assertEquals(List.of(ResourceId.id("terralite:tools/stone_probe")), scriptedTools.entries());
        assertEquals(9, spawnArea.chunkPositions().size());
    }

    @Test
    void loadsApiImplementationExamplePack() throws Exception {
        ContentPack example = new ContentPackLoader().load(repoExamplesRoot().resolve("api-implementation"));

        GameContentLoadReport report = new GameContentLoader().load(List.of(example));

        Block limestone = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "example:limestone"));
        Item shard = report.gameData().get(ResourceKey.of(TerraliteRegistries.ITEMS, "example:limestone_shard"));
        Biome fields = report.gameData().get(ResourceKey.of(TerraliteRegistries.BIOMES, "example:limestone_fields"));
        Tag naturalBlocks = report.gameData().get(ResourceKey.of(TerraliteRegistries.TAGS, "example:natural_blocks"));
        CreativeCategory naturalItems =
                report.gameData().get(ResourceKey.of(TerraliteRegistries.CREATIVE_CATEGORIES, "example:natural_items"));
        CreativeCategory naturalBlocksCategory =
                report.gameData().get(ResourceKey.of(TerraliteRegistries.CREATIVE_CATEGORIES, "example:natural_blocks"));

        assertEquals(1, report.startupScripts().executedScripts());
        assertEquals("Polished Limestone", limestone.properties().displayName());
        assertEquals(1.5f, limestone.properties().hardness());
        assertEquals(List.of(ResourceId.id("example:natural_blocks")), limestone.properties().categories());
        assertEquals("Limestone Shard", shard.properties().displayName());
        assertEquals(List.of(ResourceId.id("example:natural_items")), shard.properties().categories());
        assertEquals("Limestone Fields", fields.properties().name());
        assertEquals(List.of(
                ResourceId.id("example:limestone"),
                ResourceId.id("example:limestone_grass"),
                ResourceId.id("example:limestone_soil")
        ), naturalBlocks.members());
        assertEquals(ResourceId.id("example:limestone_shard"), naturalItems.icon());
        assertEquals(ResourceId.id("example:limestone"), naturalBlocksCategory.icon());
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

    private static Path repoExamplesRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = workingDirectory.resolve("examples");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return workingDirectory.resolve("../examples").normalize();
    }
}
