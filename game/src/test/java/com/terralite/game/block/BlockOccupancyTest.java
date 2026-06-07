package com.terralite.game.block;

import com.terralite.core.registry.ResourceId;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockOccupancyTest {
    @Test
    void rotatesOffsetsAroundOriginFromFacingState() {
        BlockOccupancy occupancy = new BlockOccupancy(
                List.of(BlockPos.of(0, 0, 0), BlockPos.of(1, 0, 0)),
                "facing"
        );

        assertEquals(List.of(BlockPos.of(0, 0, 0), BlockPos.of(0, 0, 1)),
                occupancy.offsetsFor(state("east")));
        assertEquals(List.of(BlockPos.of(0, 0, 0), BlockPos.of(-1, 0, 0)),
                occupancy.offsetsFor(state("south")));
        assertEquals(List.of(BlockPos.of(0, 0, 0), BlockPos.of(0, 0, -1)),
                occupancy.offsetsFor(state("west")));
    }

    private static BlockState state(String facing) {
        return new BlockState(ResourceId.id("terralite:double_stone"), Map.of("facing", facing));
    }
}
