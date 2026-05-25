package com.terralite.engine.weather;

import com.terralite.engine.simulation.SimulationTick;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeatherCycleSystemTest {
    @Test
    void weatherCycleAdvancesAfterInterval() {
        WeatherState weather = new WeatherState();
        WeatherCycleSystem system = new WeatherCycleSystem(
            weather,
            Duration.ofSeconds(1),
            List.of(WeatherType.CLEAR, WeatherType.RAIN, WeatherType.STORM)
        );

        system.tick(new SimulationTick(1, Duration.ofMillis(999), Duration.ofMillis(999)));
        assertEquals(WeatherType.CLEAR, weather.type());

        system.tick(new SimulationTick(2, Duration.ofMillis(1), Duration.ofSeconds(1)));
        assertEquals(WeatherType.RAIN, weather.type());
        assertEquals(0.6, weather.intensity());

        system.tick(new SimulationTick(3, Duration.ofSeconds(2), Duration.ofSeconds(3)));
        assertEquals(WeatherType.CLEAR, weather.type());
        assertEquals(0.0, weather.intensity());
    }

    @Test
    void weatherCycleRejectsInvalidConfiguration() {
        WeatherState weather = new WeatherState();

        assertThrows(IllegalArgumentException.class,
            () -> new WeatherCycleSystem(weather, Duration.ZERO, List.of(WeatherType.CLEAR)));
        assertThrows(IllegalArgumentException.class,
            () -> new WeatherCycleSystem(weather, Duration.ofSeconds(1), List.of()));
    }
}
