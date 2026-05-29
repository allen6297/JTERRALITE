package com.terralite.content.json;

public enum JsonContentRoot {
    DATA("data"),
    ASSETS("assets"),
    Scripts("scripts");

    private final String directoryName;

    JsonContentRoot(String directoryName) {
        this.directoryName = directoryName;
    }

    public String directoryName() {
        return directoryName;
    }
}
