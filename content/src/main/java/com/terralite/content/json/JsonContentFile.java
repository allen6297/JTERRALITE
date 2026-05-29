package com.terralite.content.json;

import com.terralite.core.registry.ResourceId;

import java.nio.file.Path;
import java.util.Objects;

public record JsonContentFile(JsonContentRoot root, String type, ResourceId id, Path path) {
    public JsonContentFile {
        Objects.requireNonNull(root, "root");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Content file type cannot be blank");
        }
        Objects.requireNonNull(id, "id");
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
