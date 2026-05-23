package com.terralite.core.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class GameData {
    private final Map<RegistryKey<?>, FrozenRegistry<?>> registries;

    GameData(Map<RegistryKey<?>, FrozenRegistry<?>> registries) {
        this.registries = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(registries, "registries")));
    }

    public <T> FrozenRegistry<T> registry(RegistryKey<T> key) {
        FrozenRegistry<?> registry = registries.get(key);
        if (registry == null) {
            throw new IllegalArgumentException("Missing registry: " + key.id());
        }
        return cast(key, registry);
    }

    public <T> T get(ResourceKey<T> key) {
        return registry(key.registry()).require(key);
    }

    @SuppressWarnings("unchecked")
    private static <T> FrozenRegistry<T> cast(RegistryKey<T> key, FrozenRegistry<?> registry) {
        if (!registry.key().type().equals(key.type())) {
            throw new IllegalStateException("Registry type mismatch for " + key.id());
        }
        return (FrozenRegistry<T>) registry;
    }
}
