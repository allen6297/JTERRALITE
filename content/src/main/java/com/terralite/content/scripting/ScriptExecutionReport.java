package com.terralite.content.scripting;

import java.util.List;
import java.util.Objects;

public record ScriptExecutionReport(int executedScripts, List<ScriptExecutionMessage> messages) {
    public ScriptExecutionReport {
        if (executedScripts < 0) {
            throw new IllegalArgumentException("Executed script count cannot be negative");
        }
        messages = List.copyOf(Objects.requireNonNull(messages, "messages"));
    }

    public static ScriptExecutionReport empty() {
        return new ScriptExecutionReport(0, List.of());
    }
}
