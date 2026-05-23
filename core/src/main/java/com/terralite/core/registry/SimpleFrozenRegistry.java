package com.terralite.core.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class SimpleFrozenRegistry<T> implements FrozenRegistry<T> {
    private final RegistryKey<T> key;
    private final Map<ResourceId, T> entries;

    public SimpleFrozenRegistry(RegistryKey<T> key, Map<ResourceId, T> entries) {
        this.key = Objects.requireNonNull(key, "key");
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(entries, "entries")));
    }

    @Override
    public RegistryKey<T> key() {
        return key;
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
        return entries.keySet();
    }

    @Override
    public Collection<T> values() {
        return entries.values();
    }

    @Override
    public int size() {
        return entries.size();
    }
}
