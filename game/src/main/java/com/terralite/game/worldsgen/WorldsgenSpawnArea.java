package com.terralite.game.worldsgen;

import com.terralite.engine.chunk.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record WorldsgenSpawnArea(ChunkPos center, Radius radius) {
    public WorldsgenSpawnArea {
        center = Objects.requireNonNull(center, "center");
        radius = Objects.requireNonNull(radius, "radius");
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ChunkPos> chunkPositions() {
        List<ChunkPos> positions = new ArrayList<>();
        for (int y = center.y() - radius.vertical(); y <= center.y() + radius.vertical(); y++) {
            for (int x = center.x() - radius.horizontal(); x <= center.x() + radius.horizontal(); x++) {
                for (int z = center.z() - radius.horizontal(); z <= center.z() + radius.horizontal(); z++) {
                    positions.add(ChunkPos.of(x, y, z));
                }
            }
        }
        return List.copyOf(positions);
    }

    public record Radius(int horizontal, int vertical) {
        public Radius {
            if (horizontal < 0) {
                throw new IllegalArgumentException("Horizontal radius cannot be negative");
            }
            if (vertical < 0) {
                throw new IllegalArgumentException("Vertical radius cannot be negative");
            }
        }
    }

    public static final class Builder {
        private ChunkPos center = ChunkPos.of(0, 0, 0);
        private int horizontalRadius = 1;
        private int verticalRadius = 0;

        private Builder() {
        }

        public Builder center(int x, int y, int z) {
            this.center = ChunkPos.of(x, y, z);
            return this;
        }

        public Builder radius(int horizontal, int vertical) {
            this.horizontalRadius = horizontal;
            this.verticalRadius = vertical;
            return this;
        }

        public WorldsgenSpawnArea build() {
            return new WorldsgenSpawnArea(center, new Radius(horizontalRadius, verticalRadius));
        }
    }
}
