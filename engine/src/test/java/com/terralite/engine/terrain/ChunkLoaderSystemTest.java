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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkLoaderSystemTest {
    @Test
    void chunkPosForUsesFloorDivision() {
        Entity target = new Entity(com.terralite.engine.entity.EntityId.of(1));
        ChunkLoaderSystem system = new ChunkLoaderSystem(target.id(), 16, ChunkLoadRadius.horizontal(0));

        assertEquals(ChunkPos.of(1, 0, -1), system.chunkPosFor(new Transform(20.0, 0.0, -0.1)));
        assertEquals(ChunkPos.of(-1, -1, -2), system.chunkPosFor(new Transform(-0.1, -0.1, -16.1)));
    }

    @Test
    void loaderCreatesChunksAroundTargetInHorizontalRadius() {
        World world = new World();
        Entity target = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, new Transform(20.0, 0.0, 0.0));
        ChunkLoaderSystem system = new ChunkLoaderSystem(target.id(), 16, ChunkLoadRadius.horizontal(1));

        system.tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(9, world.chunks().size());
        assertTrue(world.containsChunk(ChunkPos.of(0, 0, -1)));
        assertTrue(world.containsChunk(ChunkPos.of(1, 0, 0)));
        assertTrue(world.containsChunk(ChunkPos.of(2, 0, 1)));
    }

    @Test
    void loaderCreatesChunksAcrossVerticalRadius() {
        World world = new World();
        Entity target = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);
        ChunkLoaderSystem system = new ChunkLoaderSystem(target.id(), 16, ChunkLoadRadius.cubic(1));

        system.tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertEquals(27, world.chunks().size());
        assertTrue(world.containsChunk(ChunkPos.of(0, -1, 0)));
        assertTrue(world.containsChunk(ChunkPos.of(0, 1, 0)));
    }

    @Test
    void loaderDoesNotReplaceExistingChunks() {
        World world = new World();
        Entity target = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);
        Chunk existing = world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        ChunkLoaderSystem system = new ChunkLoaderSystem(target.id(), 16, ChunkLoadRadius.horizontal(0));

        system.tick(world, new SimulationTick(1, Duration.ofMillis(50), Duration.ofMillis(50)));

        assertSame(existing, world.requireChunk(ChunkPos.of(0, 0, 0)));
        assertEquals(List.of(ChunkPos.of(0, 0, 0)), List.copyOf(world.chunkPositions()));
    }

    @Test
    void loaderRequiresPositiveChunkSize() {
        Entity target = new Entity(com.terralite.engine.entity.EntityId.of(1));

        assertThrows(IllegalArgumentException.class,
            () -> new ChunkLoaderSystem(target.id(), 0, ChunkLoadRadius.horizontal(0)));
    }
}
