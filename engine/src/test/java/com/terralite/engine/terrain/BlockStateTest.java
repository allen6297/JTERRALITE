package com.terralite.engine.terrain;

import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStateTest {
    @Test
    void blockStateStoresResourceId() {
        BlockState stone = BlockState.of("terralite:stone");

        assertEquals(ResourceId.id("terralite:stone"), stone.id());
        assertFalse(stone.isAir());
    }

    @Test
    void airStateIsRecognized() {
        assertTrue(BlockState.AIR.isAir());
    }
}
