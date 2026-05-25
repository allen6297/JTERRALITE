package com.terralite.engine.physics;

public record Aabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public Aabb {
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

    public boolean intersects(Aabb other) {
        return minX <= other.maxX && maxX >= other.minX
            && minY <= other.maxY && maxY >= other.minY
            && minZ <= other.maxZ && maxZ >= other.minZ;
    }
}
