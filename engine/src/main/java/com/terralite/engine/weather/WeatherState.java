package com.terralite.engine.weather;

import java.util.Objects;

public final class WeatherState {
    private WeatherType type;
    private double intensity;

    public WeatherState() {
        this(WeatherType.CLEAR, 0.0);
    }

    public WeatherState(WeatherType type, double intensity) {
        set(type, intensity);
    }

    public WeatherType type() {
        return type;
    }

    public double intensity() {
        return intensity;
    }

    public void set(WeatherType type, double intensity) {
        this.type = Objects.requireNonNull(type, "type");
        if (intensity < 0.0 || intensity > 1.0) {
            throw new IllegalArgumentException("Weather intensity must be between 0 and 1");
        }
        this.intensity = intensity;
    }
}
