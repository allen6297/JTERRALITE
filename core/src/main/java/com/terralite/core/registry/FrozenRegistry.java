package com.terralite.core.registry;

public interface FrozenRegistry<T> extends Registry<T> {
    @Override
    default boolean isFrozen() {
        return true;
    }
}
