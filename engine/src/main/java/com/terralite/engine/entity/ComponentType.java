package com.terralite.engine.entity;

import java.util.Objects;

public record ComponentType<T>(String name, Class<T> type) {
    public ComponentType {
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("Component type name cannot be blank");
        }
        Objects.requireNonNull(type, "type");
    }

    public static <T> ComponentType<T> of(String name, Class<T> type) {
        return new ComponentType<>(name, type);
    }
}
