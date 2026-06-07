package com.terralite.engine.terrain;

import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactBlockStorageTest {
    private static final BlockState STONE = BlockState.of("terralite:stone");
    private static final Map<BlockState, Integer> IDS = Map.of(
            BlockState.AIR, 0,
            STONE, 1
    );
    private static final List<BlockState> STATES = List.of(BlockState.AIR, STONE);

    @Test
    void storesCompactStateIdsInternally() {
        CompactBlockStorage storage = storage();
        BlockPos pos = BlockPos.of(1, 2, 3);

        storage.set(pos, STONE);

        assertEquals(1, storage.getStateId(pos));
        assertEquals(STONE, storage.get(pos));
        assertTrue(storage.contains(pos));
    }

    @Test
    void settingAirRemovesStoredStateId() {
        CompactBlockStorage storage = storage();
        BlockPos pos = BlockPos.of(1, 2, 3);

        storage.setStateId(pos, 1);
        storage.setStateId(pos, 0);

        assertFalse(storage.contains(pos));
        assertEquals(BlockState.AIR, storage.get(pos));
        assertEquals(0, storage.getStateId(pos));
    }

    @Test
    void rejectsUnknownBlockStates() {
        CompactBlockStorage storage = storage();

        assertThrows(IllegalArgumentException.class,
                () -> storage.set(BlockPos.of(0, 0, 0), new BlockState(ResourceId.id("terralite:dirt"))));
    }

    private static CompactBlockStorage storage() {
        return new CompactBlockStorage(
                state -> {
                    Integer id = IDS.get(state);
                    if (id == null) {
                        throw new IllegalArgumentException("Unknown state: " + state);
                    }
                    return id;
                },
                STATES::get,
                0
        );
    }
}
