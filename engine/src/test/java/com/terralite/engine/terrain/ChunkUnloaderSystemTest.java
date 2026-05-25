package com.terralite.engine.terrain;

import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkUnloaderSystemTest {
    @Test
    void unloaderKeepsChunksInsideRadiusAndRemovesOutsideChunks() {
        World world = new World();
        Entity target = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.putChunk(new Chunk(ChunkPos.of(1, 0, 1)));
        world.putChunk(new Chunk(ChunkPos.of(2, 0, 0)));
        world.putChunk(new Chunk(ChunkPos.of(0, 1, 0)));

        ChunkUnloaderSystem system = new ChunkUnloaderSystem(target.id(), 16, ChunkLoadRadius.horizontal(1));
        system.tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(Set.of(ChunkPos.of(0, 0, 0), ChunkPos.of(1, 0, 1)), Set.copyOf(world.chunkPositions()));
        assertFalse(world.containsChunk(ChunkPos.of(2, 0, 0)));
        assertFalse(world.containsChunk(ChunkPos.of(0, 1, 0)));
    }

    @Test
    void unloaderUsesCurrentTargetChunk() {
        World world = new World();
        Entity target = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, new Transform(32.0, 0.0, 0.0));
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.putChunk(new Chunk(ChunkPos.of(2, 0, 0)));

        ChunkUnloaderSystem system = new ChunkUnloaderSystem(target.id(), 16, ChunkLoadRadius.horizontal(0));
        system.tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertTrue(world.containsChunk(ChunkPos.of(2, 0, 0)));
        assertFalse(world.containsChunk(ChunkPos.of(0, 0, 0)));
    }

    @Test
    void unloaderRequiresPositiveChunkSize() {
        Entity target = new Entity(com.terralite.engine.entity.EntityId.of(1));

        assertThrows(IllegalArgumentException.class,
            () -> new ChunkUnloaderSystem(target.id(), 0, ChunkLoadRadius.horizontal(0)));
    }
}
