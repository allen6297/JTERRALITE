package com.terralite.engine.simulation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FixedTimestepSimulationTest {

    @Test
    fun `advance accumulates time and runs whole ticks only`() {
        val ticks = mutableListOf<SimulationTick>()
        val sim = FixedTimestepSimulation.builder()
            .tickDeltaMillis(50)
            .addSystem { tick -> ticks += tick }
            .build()

        assertEquals(0, sim.advance(49_000_000L))
        assertEquals(49_000_000L, sim.accumulator())

        assertEquals(1, sim.advance(1_000_000L))
        assertEquals(1L, sim.tick())
        assertEquals(0L, sim.accumulator())
        assertEquals(50_000_000L, sim.totalTime())
        assertEquals(SimulationTick(1, 50_000_000L, 50_000_000L), ticks.first())
    }

    @Test
    fun `advance runs multiple ticks in order`() {
        val tickIndexes = mutableListOf<Long>()
        val sim = FixedTimestepSimulation.builder()
            .tickDeltaMillis(20)
            .addSystem { tick -> tickIndexes += tick.index }
            .build()

        assertEquals(3, sim.advance(75_000_000L))
        assertEquals(listOf(1L, 2L, 3L), tickIndexes)
        assertEquals(15_000_000L, sim.accumulator())
    }

    @Test
    fun `advance clamps large elapsed times`() {
        val tickIndexes = mutableListOf<Long>()
        val sim = FixedTimestepSimulation.builder()
            .tickDeltaMillis(10)
            .maxTicksPerAdvance(3)
            .addSystem { tick -> tickIndexes += tick.index }
            .build()

        assertEquals(3, sim.advance(1_000_000_000L))
        assertEquals(listOf(1L, 2L, 3L), tickIndexes)
        assertEquals(10_000_000L - 1L, sim.accumulator())
    }

    @Test
    fun `advance rejects negative elapsed`() {
        val sim = FixedTimestepSimulation.builder().build()
        assertThrows(IllegalArgumentException::class.java) { sim.advance(-1L) }
    }
}
