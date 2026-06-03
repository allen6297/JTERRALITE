package com.terralite.game.scripting;

import com.terralite.content.scripting.StartupScriptGlobal;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.item.Item;
import com.terralite.game.tag.Tag;

import java.util.List;
import java.util.Objects;

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
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(blockRegistry, "blockRegistry");
        Objects.requireNonNull(itemRegistry, "itemRegistry");
        Objects.requireNonNull(biomeRegistry, "biomeRegistry");
        Objects.requireNonNull(tagRegistry, "tagRegistry");
        Objects.requireNonNull(creativeCategoryRegistry, "creativeCategoryRegistry");

        StartupEventsScriptApi startupEvents =
                new StartupEventsScriptApi(
                        namespace,
                        blockRegistry,
                        itemRegistry,
                        biomeRegistry,
                        tagRegistry,
                        creativeCategoryRegistry
                );
        RegistryScriptApi registry = new RegistryScriptApi(blockRegistry, itemRegistry, biomeRegistry);

        return List.of(
                new StartupScriptGlobal("StartupEvents", startupEvents),
                new StartupScriptGlobal("Registry", registry)
        );
    }
}
