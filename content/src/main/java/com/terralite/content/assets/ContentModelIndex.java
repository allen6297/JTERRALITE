package com.terralite.content.assets;

import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.ResourceId;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ContentModelIndex {
    private final Map<ResourceId, ContentModelAsset> models;

    private ContentModelIndex(Map<ResourceId, ContentModelAsset> models) {
        this.models = Map.copyOf(Objects.requireNonNull(models, "models"));
    }

    public static ContentModelIndex load(List<ContentPack> packs) throws IOException {
        Objects.requireNonNull(packs, "packs");
        ContentAssetScanner scanner = new ContentAssetScanner();
        Map<ResourceId, ContentModelAsset> models = new LinkedHashMap<>();
        for (ContentPack pack : packs) {
            for (ContentAsset asset : scanner.scan(pack)) {
                if (!asset.type().equals("models")) {
                    continue;
                }
                ContentModelFormat.fromExtension(asset.extension())
                        .map(format -> new ContentModelAsset(asset.id(), format, asset.path()))
                        .ifPresent(model -> models.put(model.id(), model));
            }
        }
        return new ContentModelIndex(models);
    }

    public Optional<ContentModelAsset> find(ResourceId id) {
        return Optional.ofNullable(models.get(Objects.requireNonNull(id, "id")));
    }

    public List<ContentModelAsset> models() {
        return List.copyOf(models.values());
    }

    public int size() {
        return models.size();
    }
}
