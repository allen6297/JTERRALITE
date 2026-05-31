package com.terralite.runtime.world;

import com.terralite.core.registry.GameData;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.world.World;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.game.worldsgen.WorldsgenSpawnArea;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;

import java.util.Objects;

public final class RuntimeWorldFactory {
    public static final int CHUNK_SIZE = 16;
    public static final ResourceId DEFAULT_SPAWN_AREA = ResourceId.id("terralite:spawn_area");
    public static final ResourceId DEFAULT_SURFACE_BLOCK = ResourceId.id("terralite:natural/grass_block");

    public World create(GameData gameData) {
        return create(gameData, DEFAULT_SPAWN_AREA);
    }

    public World create(GameData gameData, ResourceId spawnAreaId) {
        Objects.requireNonNull(gameData, "gameData");
        Objects.requireNonNull(spawnAreaId, "spawnAreaId");

        WorldsgenSpawnArea spawnArea = gameData.registry(TerraliteRegistries.WORLDSGEN_SPAWN_AREAS)
                .get(spawnAreaId)
                .orElseGet(() -> WorldsgenSpawnArea.builder().build());
        return create(spawnArea, new BlockState(resolveSurfaceBlock(gameData)));
    }

    public World create(WorldsgenSpawnArea spawnArea) {
        return create(spawnArea, new BlockState(DEFAULT_SURFACE_BLOCK));
    }

    public World create(WorldsgenSpawnArea spawnArea, BlockState surfaceBlock) {
        Objects.requireNonNull(spawnArea, "spawnArea");
        Objects.requireNonNull(surfaceBlock, "surfaceBlock");

        World world = new World();
        for (var pos : spawnArea.chunkPositions()) {
            world.putChunk(new Chunk(pos));
            fillSurface(world, pos.x(), pos.y(), pos.z(), surfaceBlock);
        }
        return world;
    }

    private static void fillSurface(World world, int chunkX, int chunkY, int chunkZ, BlockState surfaceBlock) {
        int startX = chunkX * CHUNK_SIZE;
        int y = chunkY * CHUNK_SIZE;
        int startZ = chunkZ * CHUNK_SIZE;
        for (int x = startX; x < startX + CHUNK_SIZE; x++) {
            for (int z = startZ; z < startZ + CHUNK_SIZE; z++) {
                world.setBlock(BlockPos.of(x, y, z), surfaceBlock);
            }
        }
    }

    private static ResourceId resolveSurfaceBlock(GameData gameData) {
        try {
            var blocks = gameData.registry(TerraliteRegistries.BLOCKS);
            if (blocks.contains(DEFAULT_SURFACE_BLOCK)) {
                return DEFAULT_SURFACE_BLOCK;
            }
            return blocks.ids().stream()
                    .findFirst()
                    .orElse(DEFAULT_SURFACE_BLOCK);
        } catch (IllegalArgumentException exception) {
            return DEFAULT_SURFACE_BLOCK;
        }
    }
}
