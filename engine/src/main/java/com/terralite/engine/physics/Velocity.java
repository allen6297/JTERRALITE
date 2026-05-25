package com.terralite.engine.physics;

public record Velocity(double x, double y, double z) {
    public static final Velocity ZERO = new Velocity(0.0, 0.0, 0.0);

    public Velocity scale(double scalar) {
        return new Velocity(x * scalar, y * scalar, z * scalar);
    }
}
