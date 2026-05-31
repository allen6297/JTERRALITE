package com.terralite.game.registry;

import com.terralite.core.registry.RegistryKey;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.tag.Tag;
import com.terralite.game.worldsgen.WorldsgenSpawnArea;

public final class TerraliteRegistries {
    public static final RegistryKey<Block> BLOCKS = RegistryKey.of("terralite:blocks", Block.class);
    public static final RegistryKey<Item> ITEMS = RegistryKey.of("terralite:items", Item.class);
    public static final RegistryKey<Biome> BIOMES = RegistryKey.of("terralite:biomes", Biome.class);
    public static final RegistryKey<Tag> TAGS = RegistryKey.of("terralite:tags", Tag.class);
    public static final RegistryKey<WorldsgenSpawnArea> WORLDSGEN_SPAWN_AREAS =
            RegistryKey.of("terralite:worldsgen_spawn_areas", WorldsgenSpawnArea.class);
    public static final RegistryKey<CreativeCategory> CREATIVE_CATEGORIES =
        RegistryKey.of("terralite:creative_categories", CreativeCategory.class);

    private TerraliteRegistries() {
    }
}
