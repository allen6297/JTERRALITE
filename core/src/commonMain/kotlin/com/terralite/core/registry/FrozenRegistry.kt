package com.terralite.core.registry

/** Immutable snapshot of a [Registry] produced by [MutableRegistry.freeze]. */
interface FrozenRegistry<T> : Registry<T> {
    override fun isFrozen(): Boolean = true
}
