package com.terralite.core.registry;

import java.util.Objects;
import java.util.regex.Pattern;

public record ResourceId(String namespace, String path) {
    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern VALID_PATH = Pattern.compile("[a-z0-9_./-]+");

    public ResourceId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");

        if (!VALID_NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid namespace: " + namespace);
        }

        if (!VALID_PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
    }

    public static ResourceId of(String namespace, String path) {
        return new ResourceId(namespace, path);
    }

    public static ResourceId id(String value) {
        return parse(value);
    }

    public static ResourceId parse(String value) {
        Objects.requireNonNull(value, "value");

        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1 || value.indexOf(':', separator + 1) != -1) {
            throw new IllegalArgumentException("Invalid resource id: " + value);
        }

        return new ResourceId(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
