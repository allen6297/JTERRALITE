package com.terralite.render.mesh;

import com.terralite.render.RenderChunk;

import java.util.Objects;

/**
 * Creates a flat XY-plane debug square for a chunk at its world-space position.
 * Each unit in chunk coordinates maps to {@link #CHUNK_UNIT} world units.
 */
public final class ChunkDebugMeshFactory {
    /** World-space units per chunk coordinate step. */
    public static final float CHUNK_UNIT = 2.0f;
    /** Half the side length of each chunk marker square, in world units. */
    public static final float CHUNK_HALF_SIZE = 0.9f;

    private ChunkDebugMeshFactory() {
    }

    public static DebugMesh create(RenderChunk chunk) {
        Objects.requireNonNull(chunk, "chunk");

        float cx = chunk.x() * CHUNK_UNIT;
        float cy = chunk.y() * CHUNK_UNIT;
        float cz = chunk.z() * CHUNK_UNIT;

        float red   = colorChannel(chunk.x(), 0);
        float green = colorChannel(chunk.y(), 37);
        float blue  = colorChannel(chunk.z(), 73);

        return DebugMesh.square(cx, cy, cz, CHUNK_HALF_SIZE, red, green, blue);
    }

    private static float colorChannel(int value, int salt) {
        int mixed = Math.floorMod(value * 53 + salt, 100);
        return 0.35f + (mixed / 100.0f) * 0.6f;
    }
}
