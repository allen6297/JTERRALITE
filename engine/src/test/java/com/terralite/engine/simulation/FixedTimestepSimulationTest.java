package com.terralite.engine.simulation;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixedTimestepSimulationTest {
    @Test
    void advanceAccumulatesTimeAndRunsWholeTicksOnly() {
        List<SimulationTick> ticks = new ArrayList<>();
        FixedTimestepSimulation simulation = FixedTimestepSimulation.builder()
            .tickDelta(Duration.ofMillis(50))
            .addSystem(ticks::add)
            .build();

        assertEquals(0, simulation.advance(Duration.ofMillis(49)));
        assertEquals(Duration.ofMillis(49), simulation.accumulator());

        assertEquals(1, simulation.advance(Duration.ofMillis(1)));
        assertEquals(1, simulation.tick());
        assertEquals(Duration.ZERO, simulation.accumulator());
        assertEquals(Duration.ofMillis(50), simulation.totalTime());
        assertEquals(new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)), ticks.getFirst());
    }

    @Test
    void advanceRunsMultipleTicksInOrder() {
        List<Long> tickIndexes = new ArrayList<>();
        FixedTimestepSimulation simulation = FixedTimestepSimulation.builder()
            .tickDelta(Duration.ofMillis(20))
            .addSystem(tick -> tickIndexes.add(tick.index()))
            .build();

        assertEquals(3, simulation.advance(Duration.ofMillis(75)));

        assertEquals(List.of(1L, 2L, 3L), tickIndexes);
        assertEquals(Duration.ofMillis(15), simulation.accumulator());
    }

    @Test
    void advanceClampsLargeElapsedTimes() {
        List<Long> tickIndexes = new ArrayList<>();
        FixedTimestepSimulation simulation = FixedTimestepSimulation.builder()
            .tickDelta(Duration.ofMillis(10))
            .maxTicksPerAdvance(3)
            .addSystem(tick -> tickIndexes.add(tick.index()))
            .build();

        assertEquals(3, simulation.advance(Duration.ofSeconds(1)));

        assertEquals(List.of(1L, 2L, 3L), tickIndexes);
        assertEquals(Duration.ofNanos(Duration.ofMillis(10).toNanos() - 1), simulation.accumulator());
    }

    @Test
    void advanceRejectsNegativeElapsedTime() {
        FixedTimestepSimulation simulation = FixedTimestepSimulation.builder().build();

        assertThrows(IllegalArgumentException.class, () -> simulation.advance(Duration.ofMillis(-1)));
    }

    @Test
    void builderRejectsInvalidTimingSettings() {
        assertThrows(IllegalArgumentException.class, () -> FixedTimestepSimulation.builder()
            .tickDelta(Duration.ZERO)
            .build());
        assertThrows(IllegalArgumentException.class, () -> FixedTimestepSimulation.builder()
            .maxTicksPerAdvance(0)
            .build());
    }
}
