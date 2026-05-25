package com.terralite.engine.weather;

import com.terralite.engine.simulation.SimulationSystem;
import com.terralite.engine.simulation.SimulationTick;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class WeatherCycleSystem implements SimulationSystem {
    private final WeatherState weather;
    private final Duration interval;
    private final List<WeatherType> cycle;
    private Duration elapsed = Duration.ZERO;
    private int index;

    public WeatherCycleSystem(WeatherState weather, Duration interval, List<WeatherType> cycle) {
        this.weather = Objects.requireNonNull(weather, "weather");
        if (Objects.requireNonNull(interval, "interval").isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("Weather interval must be positive");
        }
        this.interval = interval;
        this.cycle = List.copyOf(Objects.requireNonNull(cycle, "cycle"));
        if (this.cycle.isEmpty()) {
            throw new IllegalArgumentException("Weather cycle cannot be empty");
        }
        weather.set(this.cycle.getFirst(), intensityFor(this.cycle.getFirst()));
    }

    public static WeatherCycleSystem defaultCycle(WeatherState weather, Duration interval) {
        return new WeatherCycleSystem(weather, interval, List.of(WeatherType.CLEAR, WeatherType.RAIN, WeatherType.STORM));
    }

    @Override
    public void tick(SimulationTick tick) {
        elapsed = elapsed.plus(tick.delta());
        while (elapsed.compareTo(interval) >= 0) {
            elapsed = elapsed.minus(interval);
            index = (index + 1) % cycle.size();
            WeatherType next = cycle.get(index);
            weather.set(next, intensityFor(next));
        }
    }

    private static double intensityFor(WeatherType type) {
        return switch (type) {
            case CLEAR -> 0.0;
            case RAIN -> 0.6;
            case STORM -> 1.0;
        };
    }
}
