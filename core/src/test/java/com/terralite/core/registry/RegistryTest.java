package com.terralite.core.registry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryTest {
    private static final RegistryKey<TestBlock> BLOCKS = RegistryKey.of("terralite:blocks", TestBlock.class);
    private static final ResourceKey<TestBlock> STONE = ResourceKey.of(BLOCKS, "terralite:stone");

    @Test
    void resourceIdsParseNamespaceAndPath() {
        ResourceId id = ResourceId.parse("terralite:ore/copper");

        assertEquals("terralite", id.namespace());
        assertEquals("ore/copper", id.path());
        assertEquals("terralite:ore/copper", id.toString());
    }

    @Test
    void resourceIdsRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("Stone"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("terralite"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("terralite:"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("terralite:stone:extra"));
    }

    @Test
    void registryRegistersAndFreezesEntriesDeterministically() {
        RegistryManager manager = new RegistryManager();
        MutableRegistry<TestBlock> blocks = manager.create(BLOCKS);

        TestBlock stone = blocks.register(STONE, new TestBlock("stone"));
        blocks.register(ResourceId.id("terralite:dirt"), new TestBlock("dirt"));

        GameData gameData = manager.freeze();
        FrozenRegistry<TestBlock> frozenBlocks = gameData.registry(BLOCKS);

        assertTrue(frozenBlocks.isFrozen());
        assertEquals(stone, gameData.get(STONE));
        assertEquals(List.of(ResourceId.id("terralite:stone"), ResourceId.id("terralite:dirt")), List.copyOf(frozenBlocks.ids()));
    }

    @Test
    void registryRejectsDuplicateEntries() {
        MutableRegistry<TestBlock> blocks = new SimpleMutableRegistry<>(BLOCKS);
        blocks.register(STONE, new TestBlock("stone"));

        assertThrows(IllegalArgumentException.class, () -> blocks.register(STONE, new TestBlock("stone2")));
    }

    @Test
    void registryRejectsResourceKeysFromOtherRegistries() {
        RegistryKey<TestBlock> items = RegistryKey.of("terralite:items", TestBlock.class);
        ResourceKey<TestBlock> itemKey = ResourceKey.of(items, "terralite:stone");
        MutableRegistry<TestBlock> blocks = new SimpleMutableRegistry<>(BLOCKS);

        assertThrows(IllegalArgumentException.class, () -> blocks.register(itemKey, new TestBlock("stone")));
    }

    @Test
    void registryRejectsRegistrationAfterFreeze() {
        MutableRegistry<TestBlock> blocks = new SimpleMutableRegistry<>(BLOCKS);
        blocks.freeze();

        assertThrows(IllegalStateException.class, () -> blocks.register(STONE, new TestBlock("stone")));
    }

    private record TestBlock(String name) {
    }
}
