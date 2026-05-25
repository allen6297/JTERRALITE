package com.terralite.engine.terrain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SparseBlockStorageTest {
    @Test
    void storageReturnsAirForMissingBlocks() {
        SparseBlockStorage storage = new SparseBlockStorage();

        assertEquals(BlockState.AIR, storage.get(BlockPos.of(1, 2, 3)));
        assertEquals(BlockState.AIR, storage.remove(BlockPos.of(1, 2, 3)));
    }

    @Test
    void storageSetsAndGetsBlocks() {
        SparseBlockStorage storage = new SparseBlockStorage();
        BlockPos pos = BlockPos.of(1, 2, 3);
        BlockState stone = BlockState.of("terralite:stone");

        storage.set(pos, stone);

        assertTrue(storage.contains(pos));
        assertEquals(stone, storage.get(pos));
        assertEquals(1, storage.size());
    }

    @Test
    void settingAirRemovesStoredBlock() {
        SparseBlockStorage storage = new SparseBlockStorage();
        BlockPos pos = BlockPos.of(1, 2, 3);

        storage.set(pos, BlockState.of("terralite:stone"));
        storage.set(pos, BlockState.AIR);

        assertFalse(storage.contains(pos));
        assertEquals(BlockState.AIR, storage.get(pos));
        assertEquals(0, storage.size());
    }

    @Test
    void storagePreservesPositionInsertionOrder() {
        SparseBlockStorage storage = new SparseBlockStorage();
        BlockPos first = BlockPos.of(0, 0, 0);
        BlockPos second = BlockPos.of(1, 0, 0);

        storage.set(first, BlockState.of("terralite:stone"));
        storage.set(second, BlockState.of("terralite:dirt"));

        assertEquals(List.of(first, second), List.copyOf(storage.positions()));
    }
}
