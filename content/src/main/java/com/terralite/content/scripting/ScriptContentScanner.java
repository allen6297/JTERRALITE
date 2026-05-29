package com.terralite.content.scripting;

import com.terralite.content.pack.ContentPack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ScriptContentScanner {
    public List<ScriptContentFile> scan(ContentPack pack) throws IOException {
        Objects.requireNonNull(pack, "pack");

        List<ScriptContentFile> files = new ArrayList<>();
        for (ScriptScope scope : ScriptScope.values()) {
            scanScope(pack.root(), scope, files);
        }
        files.sort(Comparator
                .comparing((ScriptContentFile file) -> file.scope().directoryName())
                .thenComparing(file -> file.path().toString()));
        return List.copyOf(files);
    }

    private static void scanScope(Path packRoot, ScriptScope scope, List<ScriptContentFile> files) throws IOException {
        Path scopePath = packRoot.resolve("scripts").resolve(scope.directoryName());
        if (!Files.isDirectory(scopePath)) {
            return;
        }

        try (var stream = Files.walk(scopePath)) {
            stream.filter(Files::isRegularFile)
                    .filter(ScriptContentScanner::isScriptFile)
                    .map(path -> new ScriptContentFile(scope, path))
                    .forEach(files::add);
        }
    }

    private static boolean isScriptFile(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().endsWith(".js");
    }
}
