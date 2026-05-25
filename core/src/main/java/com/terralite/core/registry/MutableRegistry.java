package com.terralite.core.registry;

public interface MutableRegistry<T> extends Registry<T> {
    T register(ResourceId id, T value);

    default T register(ResourceKey<T> resourceKey, T value) {
        if (!key().equals(resourceKey.registry())) {
            throw new IllegalArgumentException("Resource key belongs to registry " + resourceKey.registry().id() + ", not " + id());
        }
        return register(resourceKey.id(), value);
    }

    FrozenRegistry<T> freeze();
}
