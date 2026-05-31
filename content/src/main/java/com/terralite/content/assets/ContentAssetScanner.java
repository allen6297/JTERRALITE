package com.terralite.content.assets;

import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.ResourceId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ContentAssetScanner {
    public List<ContentAsset> scan(ContentPack pack) throws IOException {
        Objects.requireNonNull(pack, "pack");

        Path packRoot = pack.root().toAbsolutePath().normalize();
        Path assetsRoot = packRoot.resolve("assets").normalize();
        if (!assetsRoot.startsWith(packRoot) || !Files.isDirectory(assetsRoot)) {
            return List.of();
        }

        List<ContentAsset> assets = new ArrayList<>();
        try (var stream = Files.walk(assetsRoot)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.toAbsolutePath().normalize())
                    .filter(path -> path.startsWith(assetsRoot))
                    .map(path -> toAsset(assetsRoot, pack.manifest().id().namespace(), path))
                    .flatMap(List::stream)
                    .forEach(assets::add);
        }

        assets.sort(Comparator
                .comparing(ContentAsset::type)
                .thenComparing(asset -> asset.id().toString()));
        return List.copyOf(assets);
    }

    private static List<ContentAsset> toAsset(Path assetsRoot, String namespace, Path path) {
        Path relative = assetsRoot.relativize(path);
        if (relative.getNameCount() < 2) {
            return List.of();
        }

        String type = relative.getName(0).toString();
        String idPath = stripExtension(relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/'));
        return List.of(new ContentAsset(type, ResourceId.of(namespace, idPath), path));
    }

    private static String stripExtension(String path) {
        int slash = path.lastIndexOf('/');
        int dot = path.lastIndexOf('.');
        if (dot <= slash) {
            return path;
        }
        return path.substring(0, dot);
    }
}
