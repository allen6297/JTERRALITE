package com.terralite.content.json;

import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.ResourceId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class JsonContentScanner {
    public List<JsonContentFile> scan(ContentPack pack) throws IOException {
        Objects.requireNonNull(pack, "pack");

        List<JsonContentFile> files = new ArrayList<>();
        for (JsonContentRoot root : JsonContentRoot.values()) {
            scanRoot(pack.root(), pack.manifest().id().namespace(), root, files);
        }
        files.sort(Comparator
                .comparing((JsonContentFile file) -> file.root().directoryName())
                .thenComparing(JsonContentFile::type)
                .thenComparing(file -> file.id().toString()));
        return List.copyOf(files);
    }

    private static void scanRoot(Path packRoot, String namespace, JsonContentRoot root, List<JsonContentFile> files)
            throws IOException {
        Path normalizedPackRoot = packRoot.toAbsolutePath().normalize();
        Path rootPath = normalizedPackRoot.resolve(root.directoryName()).normalize();
        if (!rootPath.startsWith(normalizedPackRoot)) {
            return;
        }
        if (!Files.isDirectory(rootPath)) {
            return;
        }

        try (var stream = Files.walk(rootPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(JsonContentScanner::isJsonFile)
                    .map(path -> path.toAbsolutePath().normalize())
                    .filter(path -> path.startsWith(rootPath))
                    .map(path -> toContentFile(rootPath, namespace, root, path))
                    .flatMap(List::stream)
                    .forEach(files::add);
        }
    }

    private static boolean isJsonFile(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().endsWith(".json");
    }

    private static List<JsonContentFile> toContentFile(Path rootPath, String namespace, JsonContentRoot root, Path path) {
        Path relative = rootPath.relativize(path);
        if (relative.getNameCount() < 2) {
            return List.of();
        }

        String type = relative.getName(0).toString();
        String idPath = stripJsonExtension(relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/'));

        return List.of(new JsonContentFile(root, type, ResourceId.of(namespace, idPath), path));
    }

    private static String stripJsonExtension(String path) {
        return path.substring(0, path.length() - ".json".length());
    }
}
