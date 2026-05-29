package com.terralite.content.pack;

import com.terralite.content.manifest.PackManifest;

import java.nio.file.Path;
import java.util.Objects;

public record ContentPack(Path root, PackManifest manifest) {
    public ContentPack {
        root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        Objects.requireNonNull(manifest, "manifest");
    }
}
