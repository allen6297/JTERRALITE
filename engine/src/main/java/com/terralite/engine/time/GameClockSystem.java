package com.terralite.engine.time;

import com.terralite.engine.simulation.SimulationSystem;
import com.terralite.engine.simulation.SimulationTick;

import java.util.Objects;

public final class GameClockSystem implements SimulationSystem {
    private final GameClock clock;

    public GameClockSystem(GameClock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void tick(SimulationTick tick) {
        clock.advance(tick.delta());
    }
}
