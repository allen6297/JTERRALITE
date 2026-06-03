package com.terralite.runtime.terrain;

import com.terralite.core.registry.ResourceId;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;

/**
 * Generates terrain for a column of blocks at (x, z) using fractal value noise.
 *
 * <p>Layer layout (relative to surface height):
 * <ul>
 *   <li>surface y       → grass</li>
 *   <li>surface y - 1..3 → dirt</li>
 *   <li>below that      → stone</li>
 * </ul>
 */
public final class TerrainGenerator {
    private static final ResourceId GRASS = ResourceId.id("terralite:natural/grass_block");
    private static final ResourceId DIRT  = ResourceId.id("terralite:natural/dirt");
    private static final ResourceId STONE = ResourceId.id("terralite:natural/stone");

    private static final double SCALE       = 80.0;  // horizontal zoom
    private static final int    BASE_HEIGHT = 4;     // average surface y
    private static final double AMPLITUDE   = 16.0;  // ± height variation
    private static final int    OCTAVES     = 5;
    private static final double PERSISTENCE = 0.5;
    private static final double LACUNARITY  = 2.0;
    private static final int    DIRT_DEPTH  = 3;
    private static final int    STONE_FLOOR = -32;   // lowest y to fill

    private final ValueNoise noise;

    public TerrainGenerator(long seed) {
        this.noise = new ValueNoise(seed);
    }

    /** Surface block height at world (x, z). */
    public int surfaceHeight(int x, int z) {
        double n = noise.fbm(x / SCALE, z / SCALE, OCTAVES, PERSISTENCE, LACUNARITY);
        return (int) Math.round(BASE_HEIGHT + (n - 0.5) * AMPLITUDE);
    }

    /**
     * Collects block placements for one column into {@code out} without touching the world.
     * Safe to call from any thread.
     */
    public void collectColumn(int x, int z, int minY, int maxY, java.util.List<BlockPlacement> out) {
        int surface = surfaceHeight(x, z);
        for (int y = Math.max(minY, STONE_FLOOR); y <= Math.min(maxY, surface); y++) {
            ResourceId blockId = (y == surface) ? GRASS : (y >= surface - DIRT_DEPTH) ? DIRT : STONE;
            out.add(new BlockPlacement(BlockPos.of(x, y, z), new BlockState(blockId)));
        }
    }
}
