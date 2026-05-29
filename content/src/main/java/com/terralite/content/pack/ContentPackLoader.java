package com.terralite.content.pack;

import com.terralite.content.manifest.PackManifest;
import com.terralite.content.manifest.PackManifestLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ContentPackLoader {
    public static final String MANIFEST_FILE = "pack.json";

    private final PackManifestLoader manifestLoader;

    public ContentPackLoader() {
        this(new PackManifestLoader());
    }

    public ContentPackLoader(PackManifestLoader manifestLoader) {
        this.manifestLoader = Objects.requireNonNull(manifestLoader, "manifestLoader");
    }

    public ContentPack load(Path root) throws IOException {
        Path normalizedRoot = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IOException("Content pack root is not a directory: " + normalizedRoot);
        }

        Path manifestPath = normalizedRoot.resolve(MANIFEST_FILE);
        if (!Files.isRegularFile(manifestPath)) {
            throw new IOException("Missing content pack manifest: " + manifestPath);
        }

        PackManifest manifest;
        try (InputStream input = Files.newInputStream(manifestPath)) {
            manifest = manifestLoader.load(input);
        }

        return new ContentPack(normalizedRoot, manifest);
    }
}
