package com.terralite.engine.physics;

public record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public Bounds {
        if (maxX < minX) {
            throw new IllegalArgumentException("Max x cannot be less than min x");
        }
        if (maxY < minY) {
            throw new IllegalArgumentException("Max y cannot be less than min y");
        }
        if (maxZ < minZ) {
            throw new IllegalArgumentException("Max z cannot be less than min z");
        }
    }

    public Transform clamp(Transform transform) {
        return new Transform(
            clamp(transform.x(), minX, maxX),
            clamp(transform.y(), minY, maxY),
            clamp(transform.z(), minZ, maxZ)
        );
    }

    public boolean contains(Transform transform) {
        return transform.x() >= minX && transform.x() <= maxX
            && transform.y() >= minY && transform.y() <= maxY
            && transform.z() >= minZ && transform.z() <= maxZ;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
