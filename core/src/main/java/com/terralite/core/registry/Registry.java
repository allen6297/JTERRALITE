package com.terralite.core.registry;

import java.util.Collection;
import java.util.Optional;

public interface Registry<T> {
    RegistryKey<T> key();

    default ResourceId id() {
        return key().id();
    }

    Optional<T> get(ResourceId id);

    default Optional<T> get(ResourceKey<T> key) {
        ensureSameRegistry(key.registry());
        return get(key.id());
    }

    T require(ResourceId id);

    default T require(ResourceKey<T> key) {
        ensureSameRegistry(key.registry());
        return require(key.id());
    }

    boolean contains(ResourceId id);

    default boolean contains(ResourceKey<T> key) {
        ensureSameRegistry(key.registry());
        return contains(key.id());
    }

    Collection<ResourceId> ids();

    Collection<T> values();

    int size();

    boolean isFrozen();

    private void ensureSameRegistry(RegistryKey<T> other) {
        if (!key().equals(other)) {
            throw new IllegalArgumentException("Resource key belongs to registry " + other.id() + ", not " + id());
        }
    }
}
