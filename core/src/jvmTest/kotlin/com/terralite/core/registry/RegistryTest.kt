package com.terralite.core.registry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RegistryTest {

    private data class TestBlock(val name: String)

    private val BLOCKS: RegistryKey<TestBlock> = RegistryKey.of("terralite:blocks")
    private val STONE: ResourceKey<TestBlock> = ResourceKey.of(BLOCKS, "terralite:stone")

    @Test
    fun `resource ids parse namespace and path`() {
        val id = ResourceId.parse("terralite:ore/copper")

        assertEquals("terralite", id.namespace)
        assertEquals("ore/copper", id.path)
        assertEquals("terralite:ore/copper", id.toString())
    }

    @Test
    fun `resource ids reject invalid values`() {
        assertThrows(IllegalArgumentException::class.java) { ResourceId.parse("Stone") }
        assertThrows(IllegalArgumentException::class.java) { ResourceId.parse("terralite") }
        assertThrows(IllegalArgumentException::class.java) { ResourceId.parse("terralite:") }
        assertThrows(IllegalArgumentException::class.java) { ResourceId.parse("terralite:stone:extra") }
    }

    @Test
    fun `registry registers and freezes entries deterministically`() {
        val manager = RegistryManager()
        val blocks = manager.create(BLOCKS)

        val stone = blocks.register(STONE, TestBlock("stone"))
        blocks.register(ResourceId.id("terralite:dirt"), TestBlock("dirt"))

        val gameData = manager.freeze()
        val frozenBlocks = gameData.registry(BLOCKS)

        assertTrue(frozenBlocks.isFrozen())
        assertEquals(stone, gameData.get(STONE))
        assertEquals(
            listOf(ResourceId.id("terralite:stone"), ResourceId.id("terralite:dirt")),
            frozenBlocks.ids().toList()
        )
    }

    @Test
    fun `registry rejects duplicate entries`() {
        val blocks = SimpleMutableRegistry(BLOCKS)
        blocks.register(STONE, TestBlock("stone"))

        assertThrows(IllegalArgumentException::class.java) {
            blocks.register(STONE, TestBlock("stone2"))
        }
    }

    @Test
    fun `registry rejects resource keys from other registries`() {
        val items: RegistryKey<TestBlock> = RegistryKey.of("terralite:items")
        val itemKey = ResourceKey.of(items, "terralite:stone")
        val blocks = SimpleMutableRegistry(BLOCKS)

        assertThrows(IllegalArgumentException::class.java) {
            blocks.register(itemKey, TestBlock("stone"))
        }
    }

    @Test
    fun `registry rejects registration after freeze`() {
        val blocks = SimpleMutableRegistry(BLOCKS)
        blocks.freeze()

        assertThrows(IllegalStateException::class.java) {
            blocks.register(STONE, TestBlock("stone"))
        }
    }
}
