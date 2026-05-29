package com.terralite.game.scripting;

import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.block.Block;
import com.terralite.game.item.Item;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StartupEventsScriptApi {
    private final MutableRegistry<Block> blockRegistry;
    private final MutableRegistry<Item> itemRegistry;

    public StartupEventsScriptApi(MutableRegistry<Block> blockRegistry, MutableRegistry<Item> itemRegistry) {
        this.blockRegistry = Objects.requireNonNull(blockRegistry, "blockRegistry");
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry");
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
}
