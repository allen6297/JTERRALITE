package com.terralite.engine.simulation;

import java.time.Duration;
import java.util.Objects;

public record SimulationTick(long index, Duration delta, Duration totalTime) {
    public SimulationTick {
        if (index <= 0) {
            throw new IllegalArgumentException("Tick index must be positive");
        }
        Objects.requireNonNull(delta, "delta");
        Objects.requireNonNull(totalTime, "totalTime");
        if (delta.isZero() || delta.isNegative()) {
            throw new IllegalArgumentException("Tick delta must be positive");
        }
        if (totalTime.isNegative()) {
            throw new IllegalArgumentException("Total time cannot be negative");
        }
    }
}
