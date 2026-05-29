package com.terralite.content.scripting;

public enum ScriptScope {
    STARTUP("startup"),
    SERVER("server"),
    CLIENT("client");

    private final String directoryName;

    ScriptScope(String directoryName) {
        this.directoryName = directoryName;
    }

    public String directoryName() {
        return directoryName;
    }
}
