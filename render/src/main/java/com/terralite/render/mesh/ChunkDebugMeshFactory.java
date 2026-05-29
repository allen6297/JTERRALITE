package com.terralite.render.mesh;

import com.terralite.render.RenderChunk;

import java.util.Objects;

public final class ChunkDebugMeshFactory {
    private static final float CHUNK_SIZE = 0.08f;
    private static final float CHUNK_SPACING = 0.1f;

    private ChunkDebugMeshFactory() {
    }

    public static DebugMesh create(RenderChunk chunk) {
        Objects.requireNonNull(chunk, "chunk");

        float x = chunk.x() * CHUNK_SPACING;
        float y = chunk.z() * CHUNK_SPACING;
        float red = colorChannel(chunk.x(), 0);
        float green = colorChannel(chunk.y(), 37);
        float blue = colorChannel(chunk.z(), 73);

        return DebugMesh.square(x, y, CHUNK_SIZE * 0.5f, red, green, blue);
    }

    private static float colorChannel(int value, int salt) {
        int mixed = Math.floorMod(value * 53 + salt, 100);
        return 0.35f + (mixed / 100.0f) * 0.6f;
    }
}
