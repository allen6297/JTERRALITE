package com.terralite.runtime.render;

import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockRaycaster;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;

import java.util.List;
import java.util.Optional;

/**
 * Builds a thin quad overlay for the currently targeted block face.
 *
 * <p>The quad is offset by {@link #OFFSET} along the face normal to prevent
 * z-fighting with the underlying block surface.
 */
public final class SelectionHighlightBuilder {
    private static final float OFFSET = 0.002f;
    private static final float R = 1.0f, G = 1.0f, B = 1.0f, A = 0.35f;
    private static final RenderChunk SELECTION_CHUNK = new RenderChunk(Integer.MIN_VALUE, 0, 0);

    private SelectionHighlightBuilder() {
    }

    public static Optional<RenderChunkMesh> build(BlockRaycaster.HitResult hit) {
        BlockPos pos = hit.blockPos();
        int nx = hit.normalX();
        int ny = hit.normalY();
        int nz = hit.normalZ();

        float ox = pos.x();
        float oy = pos.y();
        float oz = pos.z();
        float dx = OFFSET * nx;
        float dy = OFFSET * ny;
        float dz = OFFSET * nz;

        // Compute the four corners of the hit face based on the normal direction
        float[][] corners = faceCorners(nx, ny, nz);

        List<DebugVertex> verts = List.of(
                v(ox + corners[0][0] + dx, oy + corners[0][1] + dy, oz + corners[0][2] + dz, 0, 0),
                v(ox + corners[1][0] + dx, oy + corners[1][1] + dy, oz + corners[1][2] + dz, 1, 0),
                v(ox + corners[2][0] + dx, oy + corners[2][1] + dy, oz + corners[2][2] + dz, 1, 1),
                v(ox + corners[0][0] + dx, oy + corners[0][1] + dy, oz + corners[0][2] + dz, 0, 0),
                v(ox + corners[2][0] + dx, oy + corners[2][1] + dy, oz + corners[2][2] + dz, 1, 1),
                v(ox + corners[3][0] + dx, oy + corners[3][1] + dy, oz + corners[3][2] + dz, 0, 1)
        );

        return Optional.of(new RenderChunkMesh(SELECTION_CHUNK, new DebugMesh(verts)));
    }

    private static DebugVertex v(float x, float y, float z, float u, float vv) {
        return new DebugVertex(x, y, z, R, G, B, A, 0f, 0f, null);
    }

    /**
     * Returns the four corners of the face in the direction of the given unit normal,
     * in world-local block space (0–1 on each axis).
     */
    private static float[][] faceCorners(int nx, int ny, int nz) {
        if (ny == 1)  return new float[][]{{0,1,0},{1,1,0},{1,1,1},{0,1,1}}; // top
        if (ny == -1) return new float[][]{{0,0,1},{1,0,1},{1,0,0},{0,0,0}}; // bottom
        if (nx == 1)  return new float[][]{{1,0,0},{1,1,0},{1,1,1},{1,0,1}}; // east  (+X)
        if (nx == -1) return new float[][]{{0,0,1},{0,1,1},{0,1,0},{0,0,0}}; // west  (-X)
        if (nz == 1)  return new float[][]{{1,0,1},{1,1,1},{0,1,1},{0,0,1}}; // south (+Z)
        /* nz == -1 */ return new float[][]{{0,0,0},{0,1,0},{1,1,0},{1,0,0}}; // north (-Z)
    }
}
