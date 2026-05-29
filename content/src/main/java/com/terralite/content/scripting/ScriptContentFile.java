package com.terralite.content.scripting;

import java.nio.file.Path;
import java.util.Objects;

public record ScriptContentFile(ScriptScope scope, Path path) {
    public ScriptContentFile {
        Objects.requireNonNull(scope, "scope");
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
