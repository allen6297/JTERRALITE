package com.terralite.core.registry;

import java.util.Objects;

public record ResourceKey<T>(RegistryKey<T> registry, ResourceId id) {
    public ResourceKey {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(id, "id");
    }

    public static <T> ResourceKey<T> of(RegistryKey<T> registry, ResourceId id) {
        return new ResourceKey<>(registry, id);
    }

    public static <T> ResourceKey<T> of(RegistryKey<T> registry, String id) {
        return of(registry, ResourceId.parse(id));
    }
}
