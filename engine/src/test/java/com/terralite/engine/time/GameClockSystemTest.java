package com.terralite.engine.time;

import com.terralite.engine.simulation.SimulationTick;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameClockSystemTest {
    @Test
    void systemAdvancesClockByTickDelta() {
        GameClock clock = new GameClock(Duration.ofSeconds(60));
        GameClockSystem system = new GameClockSystem(clock);

        system.tick(new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(Duration.ofMillis(50), clock.elapsed());
    }
}
