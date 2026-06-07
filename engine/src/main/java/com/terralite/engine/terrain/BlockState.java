package com.terralite.engine.terrain;

import com.terralite.core.registry.ResourceId;

import java.util.Map;
import java.util.Objects;

public record BlockState(ResourceId id, Map<String, String> properties) {
    public static final BlockState AIR = new BlockState(ResourceId.id("terralite:air"));

    public BlockState(ResourceId id) {
        this(id, Map.of());
    }

    public BlockState {
        Objects.requireNonNull(id, "id");
        properties = Map.copyOf(Objects.requireNonNull(properties, "properties"));
    }

    public static BlockState of(String id) {
        return new BlockState(ResourceId.id(id));
    }

    public BlockState with(String property, String value) {
        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(value, "value");
        if (property.isBlank()) {
            throw new IllegalArgumentException("Block state property cannot be blank");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Block state value cannot be blank");
        }

        java.util.LinkedHashMap<String, String> next = new java.util.LinkedHashMap<>(properties);
        next.put(property, value);
        return new BlockState(id, next);
    }

    public String property(String property) {
        return properties.get(Objects.requireNonNull(property, "property"));
    }

    public boolean isAir() {
        return this.equals(AIR);
    }
}
