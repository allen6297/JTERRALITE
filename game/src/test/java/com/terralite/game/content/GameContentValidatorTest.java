package com.terralite.game.content;

import com.terralite.content.assets.ContentAssetIndex;
import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.content.validation.ContentValidationResult;
import com.terralite.core.registry.GameData;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.RegistryManager;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.block.BlockModel;
import com.terralite.game.block.BlockModelVariant;
import com.terralite.game.block.BlockStateDefinition;
import com.terralite.game.block.BlockTextures;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.game.tag.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameContentValidatorTest {
    @TempDir
    Path tempDir;

    private static RegistryManager baseRegistries() {
        RegistryManager registries = new RegistryManager();
        registries.create(TerraliteRegistries.BLOCKS);
        registries.create(TerraliteRegistries.ITEMS);
        registries.create(TerraliteRegistries.BIOMES);
        registries.create(TerraliteRegistries.TAGS);
        registries.create(TerraliteRegistries.WORLDSGEN_SPAWN_AREAS);
        registries.create(TerraliteRegistries.CREATIVE_CATEGORIES);
        return registries;
    }

    @Test
    void passesWhenPlacesBlockReferencesExistingBlock() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:wheat"), Block.builder().build());
        registries.requireMutable(TerraliteRegistries.ITEMS)
                .register(ResourceId.id("terralite:wheat_seeds"), Item.builder()
                        .placesBlock("terralite:wheat")
                        .build());

        assertTrue(new GameContentValidator().validate(registries.freeze()).isValid());
    }

    @Test
    void reportsMissingPlacesBlockReference() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.ITEMS)
                .register(ResourceId.id("terralite:wheat_seeds"), Item.builder()
                        .placesBlock("terralite:wheat")
                        .build());

        ContentValidationResult result = new GameContentValidator().validate(registries.freeze());

        assertEquals(1, result.issues().size());
        assertEquals("item.places_block.missing", result.issues().get(0).code());
    }

    @Test
    void ignoresItemsWithNullOrBlankPlacesBlock() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.ITEMS)
                .register(ResourceId.id("terralite:stone"), Item.builder().build());

        assertTrue(new GameContentValidator().validate(registries.freeze()).isValid());
    }

    @Test
    void passesWhenTagMembersExist() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:wheat"), Block.builder().build());
        registries.requireMutable(TerraliteRegistries.TAGS)
                .register(ResourceId.id("terralite:crops"), Tag.builder()
                        .description("Crop blocks")
                        .member("terralite:wheat")
                        .build());

        assertTrue(new GameContentValidator().validate(registries.freeze()).isValid());
    }

    @Test
    void reportsMissingTagMember() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.TAGS)
                .register(ResourceId.id("terralite:crops"), Tag.builder()
                        .description("Crop blocks")
                        .member("terralite:wheat")
                        .build());

        ContentValidationResult result = new GameContentValidator().validate(registries.freeze());

        assertEquals(1, result.issues().size());
        assertEquals("tag.member.missing", result.issues().get(0).code());
    }

    @Test
    void tagMemberCanBeItem() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.ITEMS)
                .register(ResourceId.id("terralite:wheat_seeds"), Item.builder().build());
        registries.requireMutable(TerraliteRegistries.TAGS)
                .register(ResourceId.id("terralite:seeds"), Tag.builder()
                        .description("Seed items")
                        .member("terralite:wheat_seeds")
                        .build());

        assertTrue(new GameContentValidator().validate(registries.freeze()).isValid());
    }

    @Test
    void passesWhenBiomeSurfaceBlocksExist() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:grass"), Block.builder().build());
        registries.requireMutable(TerraliteRegistries.BIOMES)
                .register(ResourceId.id("terralite:plains"), Biome.builder()
                        .name("Plains")
                        .surfaceTop("terralite:grass")
                        .build());

        assertTrue(new GameContentValidator().validate(registries.freeze()).isValid());
    }

    @Test
    void reportsMissingBiomeSurfaceBlock() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BIOMES)
                .register(ResourceId.id("terralite:plains"), Biome.builder()
                        .name("Plains")
                        .surfaceTop("terralite:grass")
                        .surfaceMiddle("terralite:dirt")
                        .surfaceBase("terralite:stone")
                        .build());

        ContentValidationResult result = new GameContentValidator().validate(registries.freeze());

        assertEquals(3, result.issues().size());
        assertTrue(result.issues().stream().allMatch(i -> i.code().equals("biome.surface.missing")));
    }

    @Test
    void ignoresBiomeSurfaceBlocksWhenNull() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BIOMES)
                .register(ResourceId.id("terralite:void"), Biome.builder().name("Void").build());

        assertTrue(new GameContentValidator().validate(registries.freeze()).isValid());
    }

    @Test
    void passesWhenCategoryReferencesResolve() {
        RegistryManager registries = baseRegistries();
        MutableRegistry<Block> blocks = registries.requireMutable(TerraliteRegistries.BLOCKS);
        MutableRegistry<Item> items = registries.requireMutable(TerraliteRegistries.ITEMS);
        MutableRegistry<CreativeCategory> categories = registries.requireMutable(TerraliteRegistries.CREATIVE_CATEGORIES);

        blocks.register(ResourceId.id("terralite:stone"), Block.builder()
                .category("terralite:building_blocks")
                .build());
        items.register(ResourceId.id("terralite:iron_pickaxe"), Item.builder()
                .category("terralite:tools_and_utilities")
                .build());
        categories.register(ResourceId.id("terralite:building_blocks"), CreativeCategory.builder()
                .title("Building Blocks")
                .icon("terralite:stone")
                .entry("terralite:stone")
                .build());
        categories.register(ResourceId.id("terralite:tools_and_utilities"), CreativeCategory.builder()
                .title("Tools and Utilities")
                .icon("terralite:iron_pickaxe")
                .entry("terralite:iron_pickaxe")
                .build());

        assertTrue(new GameContentValidator().validate(registries.freeze()).isValid());
    }

    @Test
    void reportsMissingCategoryReferences() {
        RegistryManager registries = baseRegistries();
        MutableRegistry<Block> blocks = registries.requireMutable(TerraliteRegistries.BLOCKS);
        MutableRegistry<Item> items = registries.requireMutable(TerraliteRegistries.ITEMS);
        MutableRegistry<CreativeCategory> categories = registries.requireMutable(TerraliteRegistries.CREATIVE_CATEGORIES);

        blocks.register(ResourceId.id("terralite:stone"), Block.builder()
                .category("terralite:missing_category")
                .build());
        items.register(ResourceId.id("terralite:iron_pickaxe"), Item.builder()
                .category("terralite:missing_category")
                .build());
        categories.register(ResourceId.id("terralite:building_blocks"), CreativeCategory.builder()
                .title("Building Blocks")
                .icon("terralite:missing_icon")
                .entry("terralite:missing_entry")
                .build());

        GameData gameData = registries.freeze();
        ContentValidationResult result = new GameContentValidator().validate(gameData);

        assertEquals(4, result.issues().size());
        assertEquals("block.category.missing", result.issues().get(0).code());
        assertEquals("item.category.missing", result.issues().get(1).code());
        assertEquals("category.icon.missing", result.issues().get(2).code());
        assertEquals("category.entry.missing", result.issues().get(3).code());
    }

    @Test
    void passesWhenBlockModelAndTextureAssetsExist() throws Exception {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:stone"), Block.builder()
                        .model("terralite:block/cube_all")
                        .textures(BlockTextures.all(ResourceId.id("terralite:block/stone")))
                        .build());
        ContentAssetIndex assets = ContentAssetIndex.load(List.of(writeAssetPack(
                "cube_all.json",
                List.of("stone.png")
        )));

        assertTrue(new GameContentValidator().validate(registries.freeze(), assets).isValid());
    }

    @Test
    void reportsMissingBlockModelAndTextureAssets() throws Exception {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:grass_block"), Block.builder()
                        .model("terralite:block/missing_model")
                        .textures(new BlockTextures(
                                null,
                                ResourceId.id("terralite:block/missing_top"),
                                ResourceId.id("terralite:block/dirt"),
                                ResourceId.id("terralite:block/missing_side")
                        ))
                        .build());
        ContentAssetIndex assets = ContentAssetIndex.load(List.of(writeAssetPack(
                "cube_all.json",
                List.of("dirt.png")
        )));

        ContentValidationResult result = new GameContentValidator().validate(registries.freeze(), assets);

        assertEquals(3, result.issues().size());
        assertEquals("block.model.missing", result.issues().get(0).code());
        assertEquals("block.texture.missing", result.issues().get(1).code());
        assertEquals("block.texture.missing", result.issues().get(2).code());
    }

    @Test
    void validatesBlockStateVariantAssets() throws Exception {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:wheat"), Block.builder()
                        .model("terralite:block/cross")
                        .textures(BlockTextures.all(ResourceId.id("terralite:block/wheat")))
                        .stateDefinition(new BlockStateDefinition(
                                Map.of("age", List.of("0", "7")),
                                Map.of("age", "0")
                        ))
                        .modelVariant(new BlockModelVariant(
                                Map.of("age", "7"),
                                new BlockModel(ResourceId.id("terralite:block/missing_stage")),
                                BlockTextures.all(ResourceId.id("terralite:block/missing_wheat"))
                        ))
                        .build());
        ContentAssetIndex assets = ContentAssetIndex.load(List.of(writeAssetPack(
                "cross.json",
                List.of("wheat.png")
        )));

        ContentValidationResult result = new GameContentValidator().validate(registries.freeze(), assets);

        assertEquals(2, result.issues().size());
        assertEquals("block.model.missing", result.issues().get(0).code());
        assertEquals("block.texture.missing", result.issues().get(1).code());
    }

    @Test
    void reportsStateVariantsThatDoNotMatchDeclaredSchema() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:wheat"), Block.builder()
                        .stateDefinition(new BlockStateDefinition(
                                Map.of("age", List.of("0", "7")),
                                Map.of("age", "0")
                        ))
                        .modelVariant(new BlockModelVariant(
                                Map.of("age", "banana", "facing", "north"),
                                new BlockModel(ResourceId.id("terralite:block/cross")),
                                null
                        ))
                        .build());

        ContentValidationResult result = new GameContentValidator().validate(registries.freeze());

        assertEquals(2, result.issues().size());
        assertEquals("block.state.value.invalid", result.issues().get(0).code());
        assertEquals("block.state.property.unknown", result.issues().get(1).code());
    }

    @Test
    void reportsStatePropertiesWithoutDefaults() {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:wheat"), Block.builder()
                        .stateDefinition(new BlockStateDefinition(
                                Map.of("age", List.of("0", "7")),
                                Map.of()
                        ))
                        .build());

        ContentValidationResult result = new GameContentValidator().validate(registries.freeze());

        assertEquals(1, result.issues().size());
        assertEquals("block.state.default.missing", result.issues().get(0).code());
    }

    @Test
    void assetValidationIgnoresBlocksWithoutTextureReferences() throws Exception {
        RegistryManager registries = baseRegistries();
        registries.requireMutable(TerraliteRegistries.BLOCKS)
                .register(ResourceId.id("terralite:marker"), Block.builder()
                        .model("terralite:block/missing_model")
                        .build());
        ContentAssetIndex assets = ContentAssetIndex.load(List.of(writeAssetPack(
                "cube_all.json",
                List.of()
        )));

        assertTrue(new GameContentValidator().validate(registries.freeze(), assets).isValid());
    }

    private ContentPack writeAssetPack(String modelFileName, List<String> textureFileNames) throws Exception {
        Path packRoot = tempDir.resolve("assets-" + System.nanoTime());
        Files.createDirectories(packRoot.resolve("assets/models/block"));
        Files.createDirectories(packRoot.resolve("assets/textures/block"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:assets",
              "name": "Assets",
              "version": "1.0.0"
            }
            """);
        Files.writeString(packRoot.resolve("assets/models/block").resolve(modelFileName), """
            { "type": "cube_all" }
            """);
        for (String textureFileName : textureFileNames) {
            Files.writeString(packRoot.resolve("assets/textures/block").resolve(textureFileName), "placeholder");
        }
        return new ContentPackLoader().load(packRoot);
    }
}
