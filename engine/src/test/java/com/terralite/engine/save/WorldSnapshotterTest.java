package com.terralite.engine.save;

import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldSnapshotterTest {
    @Test
    void snapshotCapturesChunkPositionsAndEntityIdsInWorldOrder() {
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.putChunk(new Chunk(ChunkPos.of(1, 0, 0)));
        Entity first = world.entities().create();
        Entity second = world.entities().create();

        WorldSnapshot snapshot = new WorldSnapshotter().snapshot(world);

        assertEquals(List.of(ChunkPos.of(0, 0, 0), ChunkPos.of(1, 0, 0)), snapshot.chunks());
        assertEquals(List.of(new EntitySnapshot(first.id()), new EntitySnapshot(second.id())), snapshot.entities());
    }

    @Test
    void snapshotDefensivelyCopiesLists() {
        List<ChunkPos> chunks = new ArrayList<>();
        List<EntitySnapshot> entities = new ArrayList<>();
        chunks.add(ChunkPos.of(0, 0, 0));
        entities.add(new EntitySnapshot(EntityId.of(1)));

        WorldSnapshot snapshot = new WorldSnapshot(chunks, entities);
        chunks.clear();
        entities.clear();

        assertEquals(List.of(ChunkPos.of(0, 0, 0)), snapshot.chunks());
        assertEquals(List.of(new EntitySnapshot(EntityId.of(1))), snapshot.entities());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.chunks().add(ChunkPos.of(1, 0, 0)));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.entities().add(new EntitySnapshot(EntityId.of(2))));
    }
}
