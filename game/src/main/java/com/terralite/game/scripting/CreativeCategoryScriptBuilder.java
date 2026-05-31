package com.terralite.game.scripting;

import com.terralite.core.registry.ResourceId;
import com.terralite.game.category.CreativeCategory;

import java.util.ArrayList;
import java.util.List;

public final class CreativeCategoryScriptBuilder {
    private final ResourceId id;
    private String title = "Category";
    private ResourceId icon = ResourceId.id("terralite:air");
    private final List<ResourceId> entries = new ArrayList<>();

    CreativeCategoryScriptBuilder(ResourceId id) {
        this.id = id;
    }

    public ResourceId id() {
        return id;
    }

    public CreativeCategoryScriptBuilder title(String title) {
        this.title = title != null && !title.isBlank() ? title : "Category";
        return this;
    }

    public CreativeCategoryScriptBuilder icon(String icon) {
        this.icon = ResourceId.id(icon);
        return this;
    }

    public CreativeCategoryScriptBuilder entry(String entry) {
        entries.add(ResourceId.id(entry));
        return this;
    }

    CreativeCategory build() {
        return CreativeCategory.builder()
                .title(title)
                .icon(icon)
                .entries(entries)
                .build();
    }
}
