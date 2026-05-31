package com.terralite.content.assets;

import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.ResourceId;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ContentAssetIndex {
    private final Map<AssetKey, ContentAsset> assets;

    private ContentAssetIndex(Map<AssetKey, ContentAsset> assets) {
        this.assets = Map.copyOf(Objects.requireNonNull(assets, "assets"));
    }

    public static ContentAssetIndex load(List<ContentPack> packs) throws IOException {
        Objects.requireNonNull(packs, "packs");
        ContentAssetScanner scanner = new ContentAssetScanner();
        Map<AssetKey, ContentAsset> assets = new LinkedHashMap<>();
        for (ContentPack pack : packs) {
            for (ContentAsset asset : scanner.scan(pack)) {
                assets.put(new AssetKey(asset.type(), asset.id()), asset);
            }
        }
        return new ContentAssetIndex(assets);
    }

    public Optional<ContentAsset> find(String type, ResourceId id) {
        return Optional.ofNullable(assets.get(new AssetKey(type, id)));
    }

    public Optional<Path> findTexture(ResourceId id) {
        return find("textures", id).map(ContentAsset::path);
    }

    public Optional<Path> findModel(ResourceId id) {
        return find("models", id).map(ContentAsset::path);
    }

    public List<ContentAsset> assets() {
        return List.copyOf(assets.values());
    }

    public int size() {
        return assets.size();
    }

    private record AssetKey(String type, ResourceId id) {
        private AssetKey {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("Asset type cannot be blank");
            }
            Objects.requireNonNull(id, "id");
        }
    }
}
