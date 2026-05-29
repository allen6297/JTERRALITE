package com.terralite.game.tag.json;

import com.terralite.core.registry.ResourceId;
import com.terralite.game.tag.Tag;

import java.util.List;

public record TagDefinition(String description, List<String> members) {
    public Tag toTag() {
        Tag.Builder builder = Tag.builder().description(description != null ? description : "");
        if (members != null) {
            for (String member : members) {
                builder.member(ResourceId.id(member));
            }
        }
        return builder.build();
    }
}
