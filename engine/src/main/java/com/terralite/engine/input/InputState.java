package com.terralite.engine.input;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class InputState {
    private final Set<InputAction> pressed = new LinkedHashSet<>();

    public void press(InputAction action) {
        pressed.add(Objects.requireNonNull(action, "action"));
    }

    public void release(InputAction action) {
        pressed.remove(Objects.requireNonNull(action, "action"));
    }

    public void setPressed(InputAction action, boolean isPressed) {
        if (isPressed) {
            press(action);
        } else {
            release(action);
        }
    }

    public boolean isPressed(InputAction action) {
        return pressed.contains(Objects.requireNonNull(action, "action"));
    }

    public void clear() {
        pressed.clear();
    }

    public Set<InputAction> pressedActions() {
        return Set.copyOf(pressed);
    }
}
