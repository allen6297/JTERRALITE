package com.terralite.render;

import com.terralite.core.registry.ResourceId;

import java.util.Objects;

public record RenderObject(ResourceId id, double x, double y, double z) {
    public RenderObject {
        Objects.requireNonNull(id, "id");
    }

    public static RenderObject of(String id, double x, double y, double z) {
        return new RenderObject(ResourceId.id(id), x, y, z);
    }
}
