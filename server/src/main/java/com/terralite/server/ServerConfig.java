package com.terralite.server;

import java.time.Duration;
import java.util.Objects;

public record ServerConfig(Duration tickDelta, int maxTicksPerAdvance) {
    public static final Duration DEFAULT_TICK_DELTA = Duration.ofMillis(50);
    public static final int DEFAULT_MAX_TICKS_PER_ADVANCE = 5;

    public ServerConfig {
        Objects.requireNonNull(tickDelta, "tickDelta");

        if (tickDelta.isZero() || tickDelta.isNegative()) {
            throw new IllegalArgumentException("Tick delta must be positive");
        }

        if (maxTicksPerAdvance <= 0) {
            throw new IllegalArgumentException("Max ticks per advance must be positive");
        }
    }

    public static ServerConfig defaults() {
        return new ServerConfig(DEFAULT_TICK_DELTA, DEFAULT_MAX_TICKS_PER_ADVANCE);
    }
}
