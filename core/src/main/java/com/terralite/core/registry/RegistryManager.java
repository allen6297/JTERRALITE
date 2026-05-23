package com.terralite.core.registry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RegistryManager {
    private final Map<RegistryKey<?>, MutableRegistry<?>> registries = new LinkedHashMap<>();
    private boolean frozen;

    public <T> MutableRegistry<T> create(RegistryKey<T> key) {
        Objects.requireNonNull(key, "key");

        if (frozen) {
            throw new IllegalStateException("Registry manager is frozen");
        }

        if (registries.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate registry: " + key.id());
        }

        MutableRegistry<T> registry = new SimpleMutableRegistry<>(key);
        registries.put(key, registry);
        return registry;
    }

    public <T> MutableRegistry<T> requireMutable(RegistryKey<T> key) {
        MutableRegistry<?> registry = registries.get(key);
        if (registry == null) {
            throw new IllegalArgumentException("Missing registry: " + key.id());
        }
        return cast(key, registry);
    }

    public GameData freeze() {
        frozen = true;

        Map<RegistryKey<?>, FrozenRegistry<?>> frozenRegistries = new LinkedHashMap<>();
        for (Map.Entry<RegistryKey<?>, MutableRegistry<?>> entry : registries.entrySet()) {
            frozenRegistries.put(entry.getKey(), entry.getValue().freeze());
        }

        return new GameData(frozenRegistries);
    }

    @SuppressWarnings("unchecked")
    private static <T> MutableRegistry<T> cast(RegistryKey<T> key, MutableRegistry<?> registry) {
        if (!registry.key().type().equals(key.type())) {
            throw new IllegalStateException("Registry type mismatch for " + key.id());
        }
        return (MutableRegistry<T>) registry;
    }
}
