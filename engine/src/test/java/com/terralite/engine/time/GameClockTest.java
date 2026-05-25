package com.terralite.engine.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameClockTest {
    @Test
    void clockTracksElapsedTimeDayAndProgress() {
        GameClock clock = new GameClock(Duration.ofSeconds(10));

        clock.advance(Duration.ofSeconds(15));

        assertEquals(Duration.ofSeconds(15), clock.elapsed());
        assertEquals(1, clock.day());
        assertEquals(0.5, clock.dayProgress());
    }

    @Test
    void clockRejectsInvalidDurations() {
        assertThrows(IllegalArgumentException.class, () -> new GameClock(Duration.ZERO));

        GameClock clock = GameClock.defaultClock();
        assertThrows(IllegalArgumentException.class, () -> clock.advance(Duration.ofNanos(-1)));
    }
}
