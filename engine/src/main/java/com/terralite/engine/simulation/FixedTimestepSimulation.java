package com.terralite.engine.simulation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FixedTimestepSimulation {
    private final Duration tickDelta;
    private final long tickDeltaNanos;
    private final int maxTicksPerAdvance;
    private final List<SimulationSystem> systems;
    private long accumulatorNanos;
    private long tick;
    private Duration totalTime = Duration.ZERO;

    private FixedTimestepSimulation(Duration tickDelta, int maxTicksPerAdvance, List<SimulationSystem> systems) {
        if (tickDelta.isZero() || tickDelta.isNegative()) {
            throw new IllegalArgumentException("Tick delta must be positive");
        }
        if (maxTicksPerAdvance <= 0) {
            throw new IllegalArgumentException("Max ticks per advance must be positive");
        }

        this.tickDelta = tickDelta;
        this.tickDeltaNanos = tickDelta.toNanos();
        this.maxTicksPerAdvance = maxTicksPerAdvance;
        this.systems = List.copyOf(systems);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Duration tickDelta() {
        return tickDelta;
    }

    public long tick() {
        return tick;
    }

    public Duration totalTime() {
        return totalTime;
    }

    public Duration accumulator() {
        return Duration.ofNanos(accumulatorNanos);
    }

    public int advance(Duration elapsed) {
        Objects.requireNonNull(elapsed, "elapsed");
        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("Elapsed time cannot be negative");
        }

        accumulatorNanos += elapsed.toNanos();

        int ticksRun = 0;
        while (accumulatorNanos >= tickDeltaNanos && ticksRun < maxTicksPerAdvance) {
            tick++;
            totalTime = totalTime.plus(tickDelta);

            SimulationTick simulationTick = new SimulationTick(tick, tickDelta, totalTime);
            for (SimulationSystem system : systems) {
                system.tick(simulationTick);
            }

            accumulatorNanos -= tickDeltaNanos;
            ticksRun++;
        }

        if (ticksRun == maxTicksPerAdvance && accumulatorNanos >= tickDeltaNanos) {
            accumulatorNanos = tickDeltaNanos - 1;
        }

        return ticksRun;
    }

    public static final class Builder {
        private final List<SimulationSystem> systems = new ArrayList<>();
        private Duration tickDelta = Duration.ofMillis(50);
        private int maxTicksPerAdvance = 5;

        private Builder() {
        }

        public Builder tickDelta(Duration tickDelta) {
            this.tickDelta = Objects.requireNonNull(tickDelta, "tickDelta");
            return this;
        }

        public Builder maxTicksPerAdvance(int maxTicksPerAdvance) {
            this.maxTicksPerAdvance = maxTicksPerAdvance;
            return this;
        }

        public Builder addSystem(SimulationSystem system) {
            systems.add(Objects.requireNonNull(system, "system"));
            return this;
        }

        public Builder addSystems(List<SimulationSystem> systems) {
            this.systems.addAll(Objects.requireNonNull(systems, "systems"));
            return this;
        }

        public FixedTimestepSimulation build() {
            return new FixedTimestepSimulation(tickDelta, maxTicksPerAdvance, systems);
        }
    }
}
