package com.terralite.server;

import java.util.Objects;

/** Server tick configuration. Durations are in nanoseconds. */
public record ServerConfig(long tickDeltaNanos, int maxTicksPerAdvance) {
    public static final long DEFAULT_TICK_DELTA_NANOS = 50_000_000L;  // 50 ms
    public static final int DEFAULT_MAX_TICKS_PER_ADVANCE = 5;

    public ServerConfig {
        if (tickDeltaNanos <= 0) {
            throw new IllegalArgumentException("Tick delta must be positive");
        }
        if (maxTicksPerAdvance <= 0) {
            throw new IllegalArgumentException("Max ticks per advance must be positive");
        }
    }

    public static ServerConfig defaults() {
        return new ServerConfig(DEFAULT_TICK_DELTA_NANOS, DEFAULT_MAX_TICKS_PER_ADVANCE);
    }
}
