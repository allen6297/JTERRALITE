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
            scanRoot(pack.root(), root, files);
        }
        files.sort(Comparator
                .comparing((JsonContentFile file) -> file.root().directoryName())
                .thenComparing(JsonContentFile::type)
                .thenComparing(file -> file.id().toString()));
        return List.copyOf(files);
    }

    private static void scanRoot(Path packRoot, JsonContentRoot root, List<JsonContentFile> files) throws IOException {
        Path rootPath = packRoot.resolve(root.directoryName());
        if (!Files.isDirectory(rootPath)) {
            return;
        }

        try (var stream = Files.walk(rootPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(JsonContentScanner::isJsonFile)
                    .map(path -> toContentFile(rootPath, root, path))
                    .flatMap(List::stream)
                    .forEach(files::add);
        }
    }

    private static boolean isJsonFile(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().endsWith(".json");
    }

    private static List<JsonContentFile> toContentFile(Path rootPath, JsonContentRoot root, Path path) {
        Path relative = rootPath.relativize(path);
        if (relative.getNameCount() < 3) {
            return List.of();
        }

        String namespace = relative.getName(0).toString();
        String type = relative.getName(1).toString();
        String idPath = stripJsonExtension(relative.subpath(2, relative.getNameCount()).toString().replace('\\', '/'));

        return List.of(new JsonContentFile(root, type, ResourceId.of(namespace, idPath), path));
    }

    private static String stripJsonExtension(String path) {
        return path.substring(0, path.length() - ".json".length());
    }
}
