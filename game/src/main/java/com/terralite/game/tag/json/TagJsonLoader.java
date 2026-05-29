package com.terralite.game.tag.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.tag.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class TagJsonLoader {
    private final ObjectMapper mapper;

    public TagJsonLoader() {
        this(defaultMapper());
    }

    public TagJsonLoader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public Tag load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return mapper.readValue(input, TagDefinition.class).toTag();
    }

    public Tag register(ResourceId id, InputStream input, MutableRegistry<Tag> tags) throws IOException {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tags, "tags");
        Tag tag = load(input);
        return tags.register(id, tag);
    }

    private static ObjectMapper defaultMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
