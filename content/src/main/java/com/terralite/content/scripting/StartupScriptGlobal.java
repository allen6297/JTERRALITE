package com.terralite.content.scripting;

public record StartupScriptGlobal(String name, Object value) {
    public StartupScriptGlobal {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Global name must not be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Global value must not be null");
        }
    }
}
