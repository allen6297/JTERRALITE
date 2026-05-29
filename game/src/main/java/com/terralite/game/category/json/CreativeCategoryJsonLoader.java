package com.terralite.game.category.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.category.CreativeCategory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class CreativeCategoryJsonLoader {
    private final ObjectMapper mapper;

    public CreativeCategoryJsonLoader() {
        this(defaultMapper());
    }

    public CreativeCategoryJsonLoader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public CreativeCategory load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return mapper.readValue(input, CreativeCategoryDefinition.class).toCategory();
    }

    public CreativeCategory register(ResourceId id, InputStream input, MutableRegistry<CreativeCategory> categories)
            throws IOException {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(categories, "categories");

        CreativeCategory category = load(input);
        return categories.register(id, category);
    }

    private static ObjectMapper defaultMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
