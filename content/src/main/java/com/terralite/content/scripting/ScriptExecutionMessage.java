package com.terralite.content.scripting;

import java.nio.file.Path;
import java.util.Objects;

public record ScriptExecutionMessage(ScriptScope scope, Path path, String message) {
    public ScriptExecutionMessage {
        Objects.requireNonNull(scope, "scope");
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Script message cannot be blank");
        }
    }
}
