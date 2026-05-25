package com.terralite.engine.weather;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeatherStateTest {
    @Test
    void weatherStateStoresTypeAndIntensity() {
        WeatherState weather = new WeatherState(WeatherType.RAIN, 0.5);

        assertEquals(WeatherType.RAIN, weather.type());
        assertEquals(0.5, weather.intensity());
    }

    @Test
    void weatherStateRejectsIntensityOutsideZeroToOne() {
        assertThrows(IllegalArgumentException.class, () -> new WeatherState(WeatherType.CLEAR, -0.1));
        assertThrows(IllegalArgumentException.class, () -> new WeatherState(WeatherType.CLEAR, 1.1));
    }
}
