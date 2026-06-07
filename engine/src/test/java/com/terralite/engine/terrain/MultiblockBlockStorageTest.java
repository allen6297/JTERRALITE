package com.terralite.engine.terrain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockBlockStorageTest {
    @Test
    void multiblockPlacementOccupiesChildCells() {
        MultiblockBlockStorage storage = storage();
        BlockPos origin = BlockPos.of(2, 0, 0);
        BlockPos child = BlockPos.of(3, 0, 0);
        BlockState doubleStone = BlockState.of("terralite:double_stone");

        storage.set(origin, doubleStone);

        assertEquals(doubleStone, storage.get(origin));
        assertEquals(doubleStone, storage.get(child));
        assertEquals(origin, storage.originOf(child));
        assertTrue(storage.contains(child));
    }

    @Test
    void multiblockPlacementRejectsOccupiedCells() {
        MultiblockBlockStorage storage = storage();
        storage.set(BlockPos.of(2, 0, 0), BlockState.of("terralite:double_stone"));

        assertThrows(IllegalStateException.class,
                () -> storage.set(BlockPos.of(3, 0, 0), BlockState.of("terralite:stone")));
    }

    @Test
    void removingAnyOccupiedCellRemovesTheWholeMultiblock() {
        MultiblockBlockStorage storage = storage();
        BlockPos origin = BlockPos.of(2, 0, 0);
        BlockPos child = BlockPos.of(3, 0, 0);
        BlockState doubleStone = BlockState.of("terralite:double_stone");
        storage.set(origin, doubleStone);

        assertEquals(doubleStone, storage.remove(child));

        assertFalse(storage.contains(origin));
        assertFalse(storage.contains(child));
        assertEquals(BlockState.AIR, storage.get(origin));
        assertEquals(BlockState.AIR, storage.get(child));
    }

    @Test
    void positionsOnlyReturnOriginsForRenderingAndIteration() {
        MultiblockBlockStorage storage = storage();
        BlockPos origin = BlockPos.of(2, 0, 0);

        storage.set(origin, BlockState.of("terralite:double_stone"));

        assertEquals(List.of(origin), List.copyOf(storage.positions()));
        assertEquals(List.of(origin, BlockPos.of(3, 0, 0)), List.copyOf(storage.occupiedPositions(origin)));
    }

    @Test
    void occupancyCanBeResolvedFromBlockStateProperties() {
        MultiblockBlockStorage storage = new MultiblockBlockStorage(new SparseBlockStorage(), state -> {
            if (state.property("facing") != null && state.property("facing").equals("east")) {
                return List.of(BlockPos.of(0, 0, 0), BlockPos.of(0, 0, 1));
            }
            return List.of(BlockPos.of(0, 0, 0), BlockPos.of(1, 0, 0));
        });
        BlockState east = BlockState.of("terralite:double_stone").with("facing", "east");

        storage.set(BlockPos.of(2, 0, 0), east);

        assertEquals(List.of(BlockPos.of(2, 0, 0), BlockPos.of(2, 0, 1)),
                List.copyOf(storage.occupiedPositions(BlockPos.of(2, 0, 0))));
    }

    private static MultiblockBlockStorage storage() {
        return new MultiblockBlockStorage(new SparseBlockStorage(), state -> {
            if (state.id().toString().equals("terralite:double_stone")) {
                return List.of(BlockPos.of(0, 0, 0), BlockPos.of(1, 0, 0));
            }
            return List.of(BlockPos.of(0, 0, 0));
        });
    }
}
