package com.terralite.content.loading;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ContentPackDiscovery {
    private final ContentPackLoader loader;

    public ContentPackDiscovery() {
        this(new ContentPackLoader());
    }

    public ContentPackDiscovery(ContentPackLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public List<ContentPack> discover(Path packsRoot) throws IOException {
        Path normalizedRoot = Objects.requireNonNull(packsRoot, "packsRoot").toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            return List.of();
        }

        try (var stream = Files.list(normalizedRoot)) {
            return stream.filter(Files::isDirectory)
                    .filter(ContentPackDiscovery::hasManifest)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(this::loadUnchecked)
                    .toList();
        }
    }

    private static boolean hasManifest(Path path) {
        return Files.isRegularFile(path.resolve(ContentPackLoader.MANIFEST_FILE));
    }

    private ContentPack loadUnchecked(Path path) {
        try {
            return loader.load(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load content pack: " + path, exception);
        }
    }
}
