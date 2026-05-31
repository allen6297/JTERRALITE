package com.terralite.game.worldsgen.json;

import com.terralite.engine.chunk.ChunkPos;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldsgenSpawnAreaJsonLoaderTest {
    @Test
    void loadsSpawnAreaChunkPositions() throws Exception {
        var spawnArea = new WorldsgenSpawnAreaJsonLoader().load(new ByteArrayInputStream("""
                {
                  "center": { "x": 2, "y": 1, "z": -3 },
                  "radius": { "horizontal": 0, "vertical": 1 }
                }
                """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(List.of(
                ChunkPos.of(2, 0, -3),
                ChunkPos.of(2, 1, -3),
                ChunkPos.of(2, 2, -3)
        ), spawnArea.chunkPositions());
    }
}
