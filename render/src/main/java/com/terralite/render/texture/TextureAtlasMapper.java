package com.terralite.render.texture;

import com.terralite.render.RenderChunkMesh;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;

import java.util.List;
import java.util.Objects;

public final class TextureAtlasMapper {
    public List<RenderChunkMesh> remap(List<RenderChunkMesh> chunkMeshes, TextureAtlas atlas) {
        Objects.requireNonNull(chunkMeshes, "chunkMeshes");
        Objects.requireNonNull(atlas, "atlas");
        return chunkMeshes.stream()
                .map(chunkMesh -> new RenderChunkMesh(chunkMesh.chunk(), remap(chunkMesh.mesh(), atlas)))
                .toList();
    }

    public DebugMesh remap(DebugMesh mesh, TextureAtlas atlas) {
        Objects.requireNonNull(mesh, "mesh");
        Objects.requireNonNull(atlas, "atlas");
        return new DebugMesh(mesh.vertices().stream()
                .map(vertex -> remap(vertex, atlas))
                .toList());
    }

    private static DebugVertex remap(DebugVertex vertex, TextureAtlas atlas) {
        if (vertex.texture() == null) {
            return vertex;
        }
        return atlas.region(vertex.texture())
                .map(region -> new DebugVertex(
                        vertex.x(),
                        vertex.y(),
                        vertex.z(),
                        vertex.red(),
                        vertex.green(),
                        vertex.blue(),
                        vertex.alpha(),
                        mapU(region, atlas, vertex.u()),
                        mapV(region, atlas, vertex.v()),
                        vertex.texture()
                ))
                .orElse(vertex);
    }

    private static float mapU(TextureRegion region, TextureAtlas atlas, float u) {
        float inset = Math.min(0.5f / atlas.width(), (region.u1() - region.u0()) * 0.5f);
        float u0 = region.u0() + inset;
        float u1 = region.u1() - inset;
        return u0 + (u1 - u0) * u;
    }

    private static float mapV(TextureRegion region, TextureAtlas atlas, float v) {
        float inset = Math.min(0.5f / atlas.height(), (region.v1() - region.v0()) * 0.5f);
        float v0 = region.v0() + inset;
        float v1 = region.v1() - inset;
        return v0 + (v1 - v0) * v;
    }
}
