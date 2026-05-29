package com.terralite.content.manifest;

import com.terralite.core.registry.ResourceId;

import java.util.List;

record PackManifestDefinition(
        String id,
        String name,
        String version,
        String description,
        List<PackDependencyDefinition> dependencies
) {
    PackManifest toManifest() {
        return new PackManifest(
                ResourceId.id(id),
                defaultString(name, id),
                defaultString(version, "1.0.0"),
                defaultString(description, ""),
                parseDependencies(dependencies)
        );
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static List<PackDependency> parseDependencies(List<PackDependencyDefinition> dependencies) {
        if (dependencies == null) {
            return List.of();
        }
        return dependencies.stream()
                .map(PackDependencyDefinition::toDependency)
                .toList();
    }
}
