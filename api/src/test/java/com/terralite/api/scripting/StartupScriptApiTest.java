package com.terralite.api.scripting;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.core.registry.ResourceKey;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.content.GameContentLoadReport;
import com.terralite.game.content.GameContentLoader;
import com.terralite.game.item.Item;
import com.terralite.game.registry.TerraliteRegistries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StartupScriptApiTest {
    @TempDir
    Path tempDir;

    private GameContentLoadReport load(List<ContentPack> packs) throws Exception {
        return new GameContentLoader().load(packs, GameStartupScriptGlobals::create);
    }

    @Test
    void startupScriptCanRegisterBlock() throws Exception {
        ContentPack pack = writePack("base", """
                StartupEvents.registry('block', function(event) {
                  event.create('base:granite')
                    .displayName('Granite')
                    .solid(true)
                    .material('rock')
                    .hardness(1.5)
                    .resistance(6.0);
                });
                """);

        GameContentLoadReport report = load(List.of(pack));

        Block granite = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "base:granite"));
        assertEquals("Granite", granite.properties().displayName());
        assertEquals(1.5f, granite.properties().hardness());
        assertEquals(6.0f, granite.properties().resistance());
        assertEquals("rock", granite.properties().material());
    }

    @Test
    void startupScriptCanRegisterItem() throws Exception {
        ContentPack pack = writePack("base", """
                StartupEvents.registry('block', function(event) {
                  event.create('base:wheat').displayName('Wheat');
                });
                StartupEvents.registry('item', function(event) {
                  event.create('base:wheat_seeds')
                    .displayName('Wheat Seeds')
                    .stackSize(99)
                    .placesBlock('base:wheat');
                });
                """);

        GameContentLoadReport report = load(List.of(pack));

        Item seeds = report.gameData().get(ResourceKey.of(TerraliteRegistries.ITEMS, "base:wheat_seeds"));
        assertEquals("Wheat Seeds", seeds.properties().displayName());
        assertEquals(99, seeds.properties().stackSize());
        assertEquals("base:wheat", seeds.properties().placesBlock());
    }

    @Test
    void startupScriptCanRegisterMultipleBlocksInOneCall() throws Exception {
        ContentPack pack = writePack("base", """
                StartupEvents.registry('block', function(event) {
                  event.create('base:stone').displayName('Stone');
                  event.create('base:dirt').displayName('Dirt').material('dirt');
                });
                """);

        GameContentLoadReport report = load(List.of(pack));

        assertEquals("Stone", report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "base:stone")).properties().displayName());
        assertEquals("Dirt", report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "base:dirt")).properties().displayName());
        assertEquals("dirt", report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "base:dirt")).properties().material());
    }

    @Test
    void startupScriptCanModifyJsonLoadedBlock() throws Exception {
        ContentPack pack = writePack("base", """
                Registry.modifyBlock('base:copper_ore', function(block) {
                  block.displayName = 'Dense Copper Ore';
                  block.hardness = 4.0;
                });
                """);
        writeBlockJson(pack, "copper_ore", """
                {"hardness": 2.0, "display_name": "Copper Ore"}
                """);

        GameContentLoadReport report = load(List.of(pack));

        Block copper = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "base:copper_ore"));
        assertEquals("Dense Copper Ore", copper.properties().displayName());
        assertEquals(4.0f, copper.properties().hardness());
    }

    @Test
    void startupScriptCanRegisterTag() throws Exception {
        ContentPack pack = writePack("base", """
                StartupEvents.registry('block', function(event) {
                  event.create('base:wheat').displayName('Wheat');
                  event.create('base:carrot').displayName('Carrot');
                });
                StartupEvents.registry('tag', function(event) {
                  event.create('base:crops')
                    .description('Crop blocks')
                    .member('base:wheat')
                    .member('base:carrot');
                });
                """);

        GameContentLoadReport report = load(List.of(pack));

        var tag = report.gameData().get(ResourceKey.of(TerraliteRegistries.TAGS, "base:crops"));
        assertEquals("Crop blocks", tag.description());
        assertEquals(2, tag.members().size());
        assertEquals("base:wheat", tag.members().get(0).toString());
        assertEquals("base:carrot", tag.members().get(1).toString());
    }

    @Test
    void startupScriptCanRegisterBiome() throws Exception {
        ContentPack pack = writePack("base", """
                StartupEvents.registry('block', function(event) {
                  event.create('base:grass').displayName('Grass');
                  event.create('base:dirt').displayName('Dirt');
                  event.create('base:stone').displayName('Stone');
                });
                StartupEvents.registry('biome', function(event) {
                  event.create('base:temperate_forest')
                    .name('Temperate Forest')
                    .priority(10)
                    .rarity(1.0)
                    .temperature(0.30, 0.70)
                    .humidity(0.40, 0.80)
                    .terrain(48, 14)
                    .surfaceTop('base:grass')
                    .surfaceMiddle('base:dirt')
                    .surfaceMiddleDepth(3)
                    .surfaceBase('base:stone');
                });
                """);

        GameContentLoadReport report = load(List.of(pack));

        var biome = report.gameData().get(ResourceKey.of(TerraliteRegistries.BIOMES, "base:temperate_forest"));
        assertEquals("Temperate Forest", biome.properties().name());
        assertEquals(10, biome.properties().priority());
        assertEquals(1.0, biome.properties().rarity());
        assertEquals(0.30, biome.properties().temperatureMin());
        assertEquals(0.70, biome.properties().temperatureMax());
        assertEquals(48, biome.properties().baseHeight());
        assertEquals(14, biome.properties().heightVariation());
        assertEquals("base:grass", biome.properties().surfaceTop());
        assertEquals("base:dirt", biome.properties().surfaceMiddle());
        assertEquals(3, biome.properties().surfaceMiddleDepth());
        assertEquals("base:stone", biome.properties().surfaceBase());
    }

    @Test
    void blockScriptBuilderStubMethodsAreChainable() throws Exception {
        ContentPack pack = writePack("base", """
                StartupEvents.registry('block', function(event) {
                  event.create('base:grass')
                    .displayName('Grass')
                    .solid(true)
                    .tintKey(true)
                    .renderType('model')
                    .model('models/blocks/grass_block.json')
                    .drops({ item: 'base:dirt', count: 1 });
                });
                """);

        GameContentLoadReport report = load(List.of(pack));

        Block grass = report.gameData().get(ResourceKey.of(TerraliteRegistries.BLOCKS, "base:grass"));
        assertEquals("Grass", grass.properties().displayName());
        assertFalse(grass.properties().transparent());
    }

    private ContentPack writePack(String directory, String startupScript) throws Exception {
        Path packRoot = tempDir.resolve(directory);
        Files.createDirectories(packRoot.resolve("scripts/startup"));
        Files.writeString(packRoot.resolve("pack.json"), """
                {
                  "id": "%s:%s",
                  "name": "%s",
                  "version": "1.0.0"
                }
                """.formatted(directory, directory, directory));
        Files.writeString(packRoot.resolve("scripts/startup/main.js"), startupScript);
        return new ContentPackLoader().load(packRoot);
    }

    private void writeBlockJson(ContentPack pack, String name, String json) throws Exception {
        Path dataDir = pack.root().resolve("data").resolve(pack.manifest().id().namespace()).resolve("blocks");
        Files.createDirectories(dataDir);
        Files.writeString(dataDir.resolve(name + ".json"), json);
    }
}
