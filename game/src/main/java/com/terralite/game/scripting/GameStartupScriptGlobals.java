package com.terralite.game.scripting;

import com.terralite.content.scripting.StartupScriptGlobal;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.game.block.Block;
import com.terralite.game.item.Item;
import com.terralite.game.tag.Tag;

import java.util.List;
import java.util.Objects;

public final class GameStartupScriptGlobals {
    private GameStartupScriptGlobals() {
    }

    public static List<StartupScriptGlobal> create(
            MutableRegistry<Block> blockRegistry,
            MutableRegistry<Item> itemRegistry,
            MutableRegistry<Tag> tagRegistry
    ) {
        Objects.requireNonNull(blockRegistry, "blockRegistry");
        Objects.requireNonNull(itemRegistry, "itemRegistry");
        Objects.requireNonNull(tagRegistry, "tagRegistry");

        StartupEventsScriptApi startupEvents = new StartupEventsScriptApi(blockRegistry, itemRegistry, tagRegistry);
        RegistryScriptApi registry = new RegistryScriptApi(blockRegistry, itemRegistry);

        return List.of(
                new StartupScriptGlobal("StartupEvents", startupEvents),
                new StartupScriptGlobal("Registry", registry)
        );
    }
}
