package com.terralite.content.scripting;

import org.mozilla.javascript.Function;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ScriptRuntimeApi {
    private final ScriptScope scope;
    private final Path path;
    private final List<ScriptExecutionMessage> messages;
    private final Consumer<Function> tickHandlerRegistrar;
    private final ServerWorldScriptApi worldApi;

    ScriptRuntimeApi(ScriptScope scope, Path path, List<ScriptExecutionMessage> messages) {
        this(scope, path, messages, null, null);
    }

    ScriptRuntimeApi(
            ScriptScope scope,
            Path path,
            List<ScriptExecutionMessage> messages,
            Consumer<Function> tickHandlerRegistrar,
            ServerWorldScriptApi worldApi
    ) {
        this.scope = Objects.requireNonNull(scope, "scope");
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.messages = Objects.requireNonNull(messages, "messages");
        this.tickHandlerRegistrar = tickHandlerRegistrar;
        this.worldApi = worldApi;
    }

    public void info(String message) {
        messages.add(new ScriptExecutionMessage(scope, path, message));
    }

    public void onTick(Function function) {
        Objects.requireNonNull(function, "function");

        if (scope != ScriptScope.SERVER || tickHandlerRegistrar == null) {
            throw new IllegalStateException("Tick handlers are only available to server scripts");
        }

        tickHandlerRegistrar.accept(function);
    }

    public ServerWorldScriptApi world() {
        if (scope != ScriptScope.SERVER || worldApi == null) {
            throw new IllegalStateException("World access is only available to server scripts");
        }

        return worldApi;
    }
}
