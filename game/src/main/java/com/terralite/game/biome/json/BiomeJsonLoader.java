package com.terralite.game.biome.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.biome.Biome;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class BiomeJsonLoader {
    private final ObjectMapper mapper;

    public BiomeJsonLoader() {
        this(defaultMapper());
    }

    public BiomeJsonLoader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public Biome load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return mapper.readValue(input, BiomeDefinition.class).toBiome();
    }

    public Biome register(ResourceId id, InputStream input, MutableRegistry<Biome> biomes) throws IOException {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(biomes, "biomes");
        Biome biome = load(input);
        return biomes.register(id, biome);
    }

    private static ObjectMapper defaultMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
