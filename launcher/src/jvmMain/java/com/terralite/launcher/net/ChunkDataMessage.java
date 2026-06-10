package com.terralite.launcher.net;

import java.util.List;

import java.util.Map;

/** Server → client: all non-air blocks in one chunk. */
public record ChunkDataMessage(int cx, int cy, int cz, List<BlockEntry> blocks) implements NetMessage {
    public record BlockEntry(int x, int y, int z, String id, Map<String, String> properties) {
        public BlockEntry(int x, int y, int z, String id) {
            this(x, y, z, id, Map.of());
        }
    }
}
