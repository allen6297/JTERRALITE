package com.terralite.engine.input;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InputBindings {
    private final Map<String, InputAction> bindings = new LinkedHashMap<>();

    public InputBindings bind(String control, InputAction action) {
        if (Objects.requireNonNull(control, "control").isBlank()) {
            throw new IllegalArgumentException("Input control cannot be blank");
        }
        bindings.put(control, Objects.requireNonNull(action, "action"));
        return this;
    }

    public Optional<InputAction> actionFor(String control) {
        return Optional.ofNullable(bindings.get(Objects.requireNonNull(control, "control")));
    }

    public boolean contains(String control) {
        return bindings.containsKey(Objects.requireNonNull(control, "control"));
    }

    public int size() {
        return bindings.size();
    }
}
