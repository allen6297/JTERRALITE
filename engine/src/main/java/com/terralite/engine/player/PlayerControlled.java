package com.terralite.engine.player;

public record PlayerControlled(double movementSpeed) {
    public PlayerControlled {
        if (movementSpeed < 0.0) {
            throw new IllegalArgumentException("Movement speed cannot be negative");
        }
    }
}
