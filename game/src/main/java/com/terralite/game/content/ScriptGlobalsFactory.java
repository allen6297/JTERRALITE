package com.terralite.game.content;

import com.terralite.content.scripting.StartupScriptGlobal;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.tag.Tag;

import java.util.List;

@FunctionalInterface
public interface ScriptGlobalsFactory {
    List<StartupScriptGlobal> create(
            String namespace,
            MutableRegistry<Block> blocks,
            MutableRegistry<Item> items,
            MutableRegistry<Biome> biomes,
            MutableRegistry<Tag> tags,
            MutableRegistry<CreativeCategory> creativeCategories
    );
}
