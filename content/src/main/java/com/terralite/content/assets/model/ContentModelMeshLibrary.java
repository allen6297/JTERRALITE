package com.terralite.content.assets.model;

import com.terralite.content.assets.ContentModelIndex;
import com.terralite.core.registry.ResourceId;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ContentModelMeshLibrary {
    private final ContentModelMeshLoader loader;

    public ContentModelMeshLibrary() {
        this(new ContentModelMeshLoader());
    }

    public ContentModelMeshLibrary(ContentModelMeshLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public Map<ResourceId, ContentModelMesh> loadSupported(ContentModelIndex index) throws IOException {
        Objects.requireNonNull(index, "index");

        Map<ResourceId, ContentModelMesh> meshes = new LinkedHashMap<>();
        for (var model : index.models()) {
            try {
                meshes.put(model.id(), loader.load(model));
            } catch (IllegalArgumentException | UnsupportedOperationException exception) {
                throw new IllegalArgumentException(
                        "Failed to load model mesh " + model.id() + " from " + model.path(),
                        exception
                );
            }
        }
        return Map.copyOf(meshes);
    }
}
