package com.terralite.core.registry;

public interface MutableRegistry<T> extends Registry<T> {
    T register(ResourceId id, T value);

    default T register(ResourceKey<T> key, T value) {
        if (!key().equals(key.registry())) {
            throw new IllegalArgumentException("Resource key belongs to registry " + key.registry().id() + ", not " + id());
        }
        return register(key.id(), value);
    }

    FrozenRegistry<T> freeze();
}
