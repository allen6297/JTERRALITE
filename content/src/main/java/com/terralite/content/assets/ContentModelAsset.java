package com.terralite.content.assets;

import com.terralite.core.registry.ResourceId;

import java.nio.file.Path;
import java.util.Objects;

public record ContentModelAsset(ResourceId id, ContentModelFormat format, Path path) {
    public ContentModelAsset {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(format, "format");
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
