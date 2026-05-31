package com.terralite.content.assets;

import java.util.Locale;
import java.util.Optional;

public enum ContentModelFormat {
    TERRALITE_JSON("json"),
    BLOCKBENCH("bbmodel"),
    WAVEFRONT_OBJ("obj");

    private final String extension;

    ContentModelFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ContentModelFormat> fromExtension(String extension) {
        String normalized = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        for (ContentModelFormat format : values()) {
            if (format.extension.equals(normalized)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }
}
