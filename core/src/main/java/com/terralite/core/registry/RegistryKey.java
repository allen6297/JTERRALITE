package com.terralite.core.registry;

import java.util.Objects;

public record RegistryKey<T>(ResourceId id, Class<T> type) {
    public RegistryKey {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }

    public static <T> RegistryKey<T> of(ResourceId id, Class<T> type) {
        return new RegistryKey<>(id, type);
    }

    public static <T> RegistryKey<T> of(String id, Class<T> type) {
        return of(ResourceId.parse(id), type);
    }
}
