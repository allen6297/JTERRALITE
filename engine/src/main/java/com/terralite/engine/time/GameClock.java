package com.terralite.engine.time;

import java.time.Duration;
import java.util.Objects;

public final class GameClock {
    private final Duration dayLength;
    private Duration elapsed = Duration.ZERO;

    public GameClock(Duration dayLength) {
        if (Objects.requireNonNull(dayLength, "dayLength").isZero() || dayLength.isNegative()) {
            throw new IllegalArgumentException("Day length must be positive");
        }
        this.dayLength = dayLength;
    }

    public static GameClock defaultClock() {
        return new GameClock(Duration.ofMinutes(20));
    }

    public Duration dayLength() {
        return dayLength;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public long day() {
        return Math.floorDiv(elapsed.toNanos(), dayLength.toNanos());
    }

    public double dayProgress() {
        return (elapsed.toNanos() % dayLength.toNanos()) / (double) dayLength.toNanos();
    }

    public void advance(Duration delta) {
        Objects.requireNonNull(delta, "delta");
        if (delta.isNegative()) {
            throw new IllegalArgumentException("Clock delta cannot be negative");
        }
        elapsed = elapsed.plus(delta);
    }
}
