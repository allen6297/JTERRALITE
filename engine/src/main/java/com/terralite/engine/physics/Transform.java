package com.terralite.engine.physics;

public record Transform(double x, double y, double z) {
    public static final Transform ORIGIN = new Transform(0.0, 0.0, 0.0);

    public Transform translate(double dx, double dy, double dz) {
        return new Transform(x + dx, y + dy, z + dz);
    }
}
