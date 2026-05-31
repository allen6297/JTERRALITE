package com.terralite.game.worldsgen.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.worldsgen.WorldsgenSpawnArea;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class WorldsgenSpawnAreaJsonLoader {
    private final ObjectMapper mapper;

    public WorldsgenSpawnAreaJsonLoader() {
        this(defaultMapper());
    }

    public WorldsgenSpawnAreaJsonLoader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public WorldsgenSpawnArea load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return mapper.readValue(input, WorldsgenSpawnAreaDefinition.class).toSpawnArea();
    }

    public WorldsgenSpawnArea register(
            ResourceId id,
            InputStream input,
            MutableRegistry<WorldsgenSpawnArea> spawnAreas
    ) throws IOException {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spawnAreas, "spawnAreas");
        return spawnAreas.register(id, load(input));
    }

    private static ObjectMapper defaultMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
