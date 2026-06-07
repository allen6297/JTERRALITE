package com.terralite.game.block;

import com.terralite.engine.terrain.BlockState;

import java.util.Map;
import java.util.Objects;

public record BlockModelVariant(Map<String, String> when, BlockModel model, BlockTextures textures) {
    public BlockModelVariant {
        when = Map.copyOf(Objects.requireNonNull(when, "when"));
        if (when.isEmpty()) {
            throw new IllegalArgumentException("Block model variant must define at least one state predicate");
        }
        model = Objects.requireNonNull(model, "model");
    }

    public boolean matches(BlockState state) {
        Objects.requireNonNull(state, "state");
        return when.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(state.property(entry.getKey())));
    }
}
