package com.terralite.api.scripting;

import com.terralite.core.registry.ResourceId;
import com.terralite.game.tag.Tag;

public final class TagScriptBuilder {
    private final ResourceId id;
    private String description = "";
    private final Tag.Builder builder;

    TagScriptBuilder(ResourceId id) {
        this.id = id;
        this.builder = Tag.builder();
    }

    public ResourceId id() {
        return id;
    }

    public TagScriptBuilder description(String description) {
        this.description = description != null ? description : "";
        builder.description(this.description);
        return this;
    }

    public TagScriptBuilder member(String memberId) {
        builder.member(ResourceId.id(memberId));
        return this;
    }

    Tag build() {
        return builder.description(description).build();
    }
}
