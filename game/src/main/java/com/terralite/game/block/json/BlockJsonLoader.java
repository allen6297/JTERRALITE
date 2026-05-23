package com.terralite.game.block.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.block.Block;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public final class BlockJsonLoader {
    private final ObjectMapper mapper;

    public BlockJsonLoader() {
        this(defaultMapper());
    }

    public BlockJsonLoader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public Block load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return mapper.readValue(input, BlockDefinition.class).toBlock();
    }

    public Block register(ResourceId id, InputStream input, MutableRegistry<Block> blocks) throws IOException {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(blocks, "blocks");

        Block block = load(input);
        return blocks.register(id, block);
    }

    private static ObjectMapper defaultMapper() {
        return new ObjectMapper()
            .findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
