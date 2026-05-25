package com.terralite.engine.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Entity {
    private final EntityId id;
    private final Map<ComponentType<?>, Object> components = new LinkedHashMap<>();

    public Entity(EntityId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public EntityId id() {
        return id;
    }

    public <T> Entity set(ComponentType<T> type, T component) {
        Objects.requireNonNull(type, "type");
        components.put(type, type.type().cast(Objects.requireNonNull(component, "component")));
        return this;
    }

    public <T> Optional<T> get(ComponentType<T> type) {
        Objects.requireNonNull(type, "type");
        return Optional.ofNullable(type.type().cast(components.get(type)));
    }

    public <T> T require(ComponentType<T> type) {
        return get(type).orElseThrow(() -> new IllegalArgumentException("Missing component: " + type.name()));
    }

    public boolean has(ComponentType<?> type) {
        return components.containsKey(Objects.requireNonNull(type, "type"));
    }

    public <T> Optional<T> remove(ComponentType<T> type) {
        Objects.requireNonNull(type, "type");
        return Optional.ofNullable(type.type().cast(components.remove(type)));
    }

    public int componentCount() {
        return components.size();
    }
}
