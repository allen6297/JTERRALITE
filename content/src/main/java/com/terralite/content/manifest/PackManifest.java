package com.terralite.content.manifest;

import com.terralite.core.registry.ResourceId;

import java.util.List;
import java.util.Objects;

public record PackManifest(
        ResourceId id,
        String name,
        int formatVersion,
        String version,
        String description,
        List<PackDependency> dependencies
) {
    public PackManifest {
        Objects.requireNonNull(id, "id");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pack name cannot be blank");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Pack version cannot be blank");
        }
        if (formatVersion < 1) {
            throw new IllegalArgumentException("Pack format version must be positive");
        }
        description = description == null ? "" : description;
        dependencies = List.copyOf(Objects.requireNonNull(dependencies, "dependencies"));
    }
}
