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
                        region.mapU(vertex.u()),
                        region.mapV(vertex.v()),
                        vertex.texture()
                ))
                .orElse(vertex);
    }
}
