package com.terralite.engine.input;

import java.util.Objects;

public record InputAction(String name) {
    public InputAction {
        if (Objects.requireNonNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("Input action name cannot be blank");
        }
    }

    public static InputAction of(String name) {
        return new InputAction(name);
    }
}
