package com.terralite.content.assets;

import com.terralite.core.registry.ResourceId;

import java.nio.file.Path;
import java.util.Objects;

public record ContentAsset(String type, ResourceId id, Path path) {
    public ContentAsset {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Asset type cannot be blank");
        }
        Objects.requireNonNull(id, "id");
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }

    public String extension() {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
