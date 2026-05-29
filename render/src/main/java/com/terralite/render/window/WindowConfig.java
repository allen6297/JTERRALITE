package com.terralite.render.window;

import java.util.Objects;

public record WindowConfig(String title, int width, int height, boolean visible, boolean openGlContext) {
    public WindowConfig {
        if (Objects.requireNonNull(title, "title").isBlank()) {
            throw new IllegalArgumentException("Window title cannot be blank");
        }
        if (width <= 0) {
            throw new IllegalArgumentException("Window width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Window height must be positive");
        }
    }

    public WindowConfig(String title, int width, int height, boolean visible) {
        this(title, width, height, visible, false);
    }

    public WindowConfig(String title, int width, int height) {
        this(title, width, height, true, false);
    }

    public static WindowConfig openGl(String title, int width, int height) {
        return new WindowConfig(title, width, height, true, true);
    }
}
