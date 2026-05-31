package com.terralite.game.scripting;

import com.terralite.core.registry.ResourceId;
import com.terralite.game.item.Item;

import java.util.ArrayList;
import java.util.List;

public final class ItemScriptBuilder {
    private final ResourceId id;
    private String displayName = "";
    private float weight = 1.0f;
    private int stackSize = 64;
    private String placesBlock = null;
    private final List<ResourceId> categories = new ArrayList<>();
    private final List<ResourceId> tags = new ArrayList<>();

    ItemScriptBuilder(ResourceId id) {
        this.id = id;
    }

    public ResourceId id() {
        return id;
    }

    public ItemScriptBuilder displayName(String displayName) {
        this.displayName = displayName != null ? displayName : "";
        return this;
    }

    public ItemScriptBuilder stackSize(int stackSize) {
        this.stackSize = stackSize;
        return this;
    }

    public ItemScriptBuilder weight(float weight) {
        this.weight = weight;
        return this;
    }

    public ItemScriptBuilder placesBlock(String blockId) {
        this.placesBlock = blockId;
        return this;
    }

    public ItemScriptBuilder category(String category) {
        categories.add(ResourceId.id(category));
        return this;
    }

    public ItemScriptBuilder tag(String tag) {
        tags.add(ResourceId.id(tag));
        return this;
    }

    List<ResourceId> tags() {
        return List.copyOf(tags);
    }

    public ItemScriptBuilder icon(String path) {
        return this;
    }

    Item build() {
        return Item.builder()
                .displayName(displayName)
                .weight(weight)
                .stackSize(stackSize)
                .placesBlock(placesBlock)
                .categories(categories)
                .build();
    }
}
