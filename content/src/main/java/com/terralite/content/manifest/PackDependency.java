package com.terralite.content.manifest;

import com.terralite.core.registry.ResourceId;

import java.util.Objects;

public record PackDependency(ResourceId id, boolean optional) {
    public PackDependency {
        Objects.requireNonNull(id, "id");
    }

    public static PackDependency required(ResourceId id) {
        return new PackDependency(id, false);
    }

    public static PackDependency optional(ResourceId id) {
        return new PackDependency(id, true);
    }
}
