package com.terralite.game.registry;

import com.terralite.core.registry.RegistryKey;
import com.terralite.game.block.Block;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.tag.Tag;

public final class TerraliteRegistries {
    public static final RegistryKey<Block> BLOCKS = RegistryKey.of("terralite:blocks", Block.class);
    public static final RegistryKey<Item> ITEMS = RegistryKey.of("terralite:items", Item.class);
    public static final RegistryKey<CreativeCategory> CREATIVE_CATEGORIES =
        RegistryKey.of("terralite:creative_categories", CreativeCategory.class);
    public static final RegistryKey<Tag> TAGS = RegistryKey.of("terralite:tags", Tag.class);

    private TerraliteRegistries() {
    }
}
