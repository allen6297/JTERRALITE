package com.terralite.launcher.net;

import java.util.Map;

/** Server → client: a single block was changed (id = block id, may be "terralite:air" for removal). */
public record BlockChangeMessage(int x, int y, int z, String id, Map<String, String> properties) implements NetMessage {
    public BlockChangeMessage(int x, int y, int z, String id) {
        this(x, y, z, id, Map.of());
    }
}
