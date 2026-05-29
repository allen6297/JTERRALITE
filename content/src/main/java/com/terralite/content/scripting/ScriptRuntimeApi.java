package com.terralite.content.scripting;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ScriptRuntimeApi {
    private final ScriptScope scope;
    private final Path path;
    private final List<ScriptExecutionMessage> messages;

    ScriptRuntimeApi(ScriptScope scope, Path path, List<ScriptExecutionMessage> messages) {
        this.scope = Objects.requireNonNull(scope, "scope");
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public void info(String message) {
        messages.add(new ScriptExecutionMessage(scope, path, message));
    }
}
