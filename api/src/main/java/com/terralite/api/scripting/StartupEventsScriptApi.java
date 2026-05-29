package com.terralite.api.scripting;

import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.item.Item;
import com.terralite.game.tag.Tag;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StartupEventsScriptApi {
    private final MutableRegistry<Block> blockRegistry;
    private final MutableRegistry<Item> itemRegistry;
    private final MutableRegistry<Biome> biomeRegistry;
    private final MutableRegistry<Tag> tagRegistry;

    public StartupEventsScriptApi(
            MutableRegistry<Block> blockRegistry,
            MutableRegistry<Item> itemRegistry,
            MutableRegistry<Biome> biomeRegistry,
            MutableRegistry<Tag> tagRegistry
    ) {
        this.blockRegistry = Objects.requireNonNull(blockRegistry, "blockRegistry");
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry");
        this.biomeRegistry = Objects.requireNonNull(biomeRegistry, "biomeRegistry");
        this.tagRegistry = Objects.requireNonNull(tagRegistry, "tagRegistry");
    }

    public void registry(String type, Function fn) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(fn, "fn");

        Context cx = Context.getCurrentContext();
        Scriptable scope = fn.getParentScope();

        switch (type) {
            case "block" -> {
                BlockRegistryEvent event = new BlockRegistryEvent();
                fn.call(cx, scope, scope, new Object[]{Context.javaToJS(event, scope)});
                for (BlockScriptBuilder builder : event.pending) {
                    blockRegistry.register(builder.id(), builder.build());
                }
            }
            case "item" -> {
                ItemRegistryEvent event = new ItemRegistryEvent();
                fn.call(cx, scope, scope, new Object[]{Context.javaToJS(event, scope)});
                for (ItemScriptBuilder builder : event.pending) {
                    itemRegistry.register(builder.id(), builder.build());
                }
            }
            case "biome" -> {
                BiomeRegistryEvent event = new BiomeRegistryEvent();
                fn.call(cx, scope, scope, new Object[]{Context.javaToJS(event, scope)});
                for (BiomeScriptBuilder builder : event.pending) {
                    biomeRegistry.register(builder.id(), builder.build());
                }
            }
            case "tag" -> {
                TagRegistryEvent event = new TagRegistryEvent();
                fn.call(cx, scope, scope, new Object[]{Context.javaToJS(event, scope)});
                for (TagScriptBuilder builder : event.pending) {
                    tagRegistry.register(builder.id(), builder.build());
                }
            }
            default -> throw new IllegalArgumentException("Unknown registry type: " + type);
        }
    }

    public static final class BlockRegistryEvent {
        private final List<BlockScriptBuilder> pending = new ArrayList<>();

        public BlockScriptBuilder create(String id) {
            Objects.requireNonNull(id, "id");
            BlockScriptBuilder builder = new BlockScriptBuilder(ResourceId.id(id));
            pending.add(builder);
            return builder;
        }
    }

    public static final class ItemRegistryEvent {
        private final List<ItemScriptBuilder> pending = new ArrayList<>();

        public ItemScriptBuilder create(String id) {
            Objects.requireNonNull(id, "id");
            ItemScriptBuilder builder = new ItemScriptBuilder(ResourceId.id(id));
            pending.add(builder);
            return builder;
        }
    }

    public static final class BiomeRegistryEvent {
        private final List<BiomeScriptBuilder> pending = new ArrayList<>();

        public BiomeScriptBuilder create(String id) {
            Objects.requireNonNull(id, "id");
            BiomeScriptBuilder builder = new BiomeScriptBuilder(ResourceId.id(id));
            pending.add(builder);
            return builder;
        }
    }

    public static final class TagRegistryEvent {
        private final List<TagScriptBuilder> pending = new ArrayList<>();

        public TagScriptBuilder create(String id) {
            Objects.requireNonNull(id, "id");
            TagScriptBuilder builder = new TagScriptBuilder(ResourceId.id(id));
            pending.add(builder);
            return builder;
        }
    }
}
