package com.terralite.core.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class Loggers {
    private Loggers() {
    }

    public static Logger get(Class<?> owner) {
        return LoggerFactory.getLogger(Objects.requireNonNull(owner, "owner"));
    }

    public static Logger get(String name) {
        return LoggerFactory.getLogger(Objects.requireNonNull(name, "name"));
    }
}
