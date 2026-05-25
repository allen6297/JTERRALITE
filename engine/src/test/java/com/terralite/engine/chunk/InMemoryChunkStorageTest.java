package com.terralite.engine.chunk;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryChunkStorageTest {
    @Test
    void storagePutsAndRetrievesChunksByPosition() {
        InMemoryChunkStorage storage = new InMemoryChunkStorage();
        ChunkPos pos = ChunkPos.of(2, 0, -3);
        Chunk chunk = new Chunk(pos);

        storage.put(chunk);

        assertTrue(storage.contains(pos));
        assertSame(chunk, storage.require(pos));
        assertEquals(chunk, storage.get(pos).orElseThrow());
    }

    @Test
    void storagePreservesInsertionOrderForPositionsAndChunks() {
        InMemoryChunkStorage storage = new InMemoryChunkStorage();
        Chunk first = new Chunk(ChunkPos.of(0, 0, 0));
        Chunk second = new Chunk(ChunkPos.of(1, 0, 0));

        storage.put(first);
        storage.put(second);

        assertEquals(List.of(first.pos(), second.pos()), List.copyOf(storage.positions()));
        assertEquals(List.of(first, second), List.copyOf(storage.chunks()));
    }

    @Test
    void storageReplacesExistingChunkAtSamePosition() {
        InMemoryChunkStorage storage = new InMemoryChunkStorage();
        ChunkPos pos = ChunkPos.of(0, 0, 0);
        Chunk first = new Chunk(pos);
        Chunk replacement = new Chunk(pos);

        storage.put(first);
        storage.put(replacement);

        assertEquals(1, storage.size());
        assertSame(replacement, storage.require(pos));
    }

    @Test
    void storageRemovesExistingChunks() {
        InMemoryChunkStorage storage = new InMemoryChunkStorage();
        Chunk chunk = new Chunk(ChunkPos.of(1, 0, 1));

        storage.put(chunk);

        assertSame(chunk, storage.remove(chunk.pos()));
        assertFalse(storage.contains(chunk.pos()));
        assertThrows(IllegalArgumentException.class, () -> storage.remove(chunk.pos()));
    }

    @Test
    void storageRequiresExistingChunks() {
        InMemoryChunkStorage storage = new InMemoryChunkStorage();

        assertFalse(storage.contains(ChunkPos.of(99, 0, 99)));
        assertThrows(IllegalArgumentException.class, () -> storage.require(ChunkPos.of(99, 0, 99)));
    }
}
