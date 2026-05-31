package com.terralite.game.worldsgen.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.terralite.game.worldsgen.WorldsgenSpawnArea;

public record WorldsgenSpawnAreaDefinition(Position center, Radius radius) {
    record Position(int x, int y, int z) {
    }

    record Radius(
            @JsonProperty(defaultValue = "1") int horizontal,
            @JsonProperty(defaultValue = "0") int vertical
    ) {
    }

    public WorldsgenSpawnArea toSpawnArea() {
        Position safeCenter = center != null ? center : new Position(0, 0, 0);
        Radius safeRadius = radius != null ? radius : new Radius(1, 0);
        return WorldsgenSpawnArea.builder()
                .center(safeCenter.x(), safeCenter.y(), safeCenter.z())
                .radius(safeRadius.horizontal(), safeRadius.vertical())
                .build();
    }
}
