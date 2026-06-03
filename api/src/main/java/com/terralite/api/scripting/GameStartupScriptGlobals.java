package com.terralite.api.scripting;

import com.terralite.content.scripting.StartupScriptGlobal;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.tag.Tag;

import java.util.List;

public final class GameStartupScriptGlobals {
    private GameStartupScriptGlobals() {
    }

    public static List<StartupScriptGlobal> create(
            String namespace,
            MutableRegistry<Block> blockRegistry,
            MutableRegistry<Item> itemRegistry,
            MutableRegistry<Biome> biomeRegistry,
            MutableRegistry<Tag> tagRegistry,
            MutableRegistry<CreativeCategory> creativeCategoryRegistry
    ) {
        return com.terralite.game.scripting.GameStartupScriptGlobals.create(
                namespace,
                blockRegistry,
                itemRegistry,
                biomeRegistry,
                tagRegistry,
                creativeCategoryRegistry
        );
    }
}
