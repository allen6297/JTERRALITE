package com.terralite.core.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SimpleMutableRegistry<T> implements MutableRegistry<T> {
    private final RegistryKey<T> key;
    private final Map<ResourceId, T> entries = new LinkedHashMap<>();
    private boolean frozen;

    public SimpleMutableRegistry(RegistryKey<T> key) {
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public RegistryKey<T> key() {
        return key;
    }

    @Override
    public T register(ResourceId id, T value) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");

        if (frozen) {
            throw new IllegalStateException("Registry is frozen: " + key.id());
        }

        if (entries.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate registry entry: " + id);
        }

        entries.put(id, value);
        return value;
    }

    @Override
    public T replace(ResourceId id, T value) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");

        if (frozen) {
            throw new IllegalStateException("Registry is frozen: " + key.id());
        }

        if (!entries.containsKey(id)) {
            throw new IllegalArgumentException("No registry entry to replace: " + id);
        }

        entries.put(id, value);
        return value;
    }

    @Override
    public Optional<T> get(ResourceId id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public T require(ResourceId id) {
        T value = entries.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Missing registry entry: " + id + " in " + key.id());
        }
        return value;
    }

    @Override
    public boolean contains(ResourceId id) {
        return entries.containsKey(id);
    }

    @Override
    public Collection<ResourceId> ids() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    @Override
    public Collection<T> values() {
        return Collections.unmodifiableCollection(entries.values());
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public FrozenRegistry<T> freeze() {
        frozen = true;
        return new SimpleFrozenRegistry<>(key, entries);
    }
}
