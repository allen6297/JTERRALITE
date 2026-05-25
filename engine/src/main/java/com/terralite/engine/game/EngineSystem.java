package com.terralite.engine.game;

public interface EngineSystem {
    default void initialize(EngineContext context) {
    }

    default void start(EngineContext context) {
    }

    default void stop(EngineContext context) {
    }
}
