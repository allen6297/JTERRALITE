package com.terralite.game.scripting;

import com.terralite.core.registry.ResourceId;
import com.terralite.game.block.Block;

public final class BlockScriptBuilder {
    private final ResourceId id;
    private String displayName = "";
    private float hardness = 1.0f;
    private float resistance = 1.0f;
    private boolean solid = true;
    private boolean transparent;
    private boolean requiresTool;
    private String material = "stone";
    private String soundType = "stone";

    BlockScriptBuilder(ResourceId id) {
        this.id = id;
    }

    public ResourceId id() {
        return id;
    }

    public BlockScriptBuilder displayName(String displayName) {
        this.displayName = displayName != null ? displayName : "";
        return this;
    }

    public BlockScriptBuilder solid(boolean solid) {
        this.solid = solid;
        return this;
    }

    public BlockScriptBuilder translucent(boolean translucent) {
        this.transparent = translucent;
        return this;
    }

    public BlockScriptBuilder material(String material) {
        this.material = material != null ? material : "stone";
        return this;
    }

    public BlockScriptBuilder hardness(float hardness) {
        this.hardness = hardness;
        return this;
    }

    public BlockScriptBuilder resistance(float resistance) {
        this.resistance = resistance;
        return this;
    }

    public BlockScriptBuilder requiresTool(boolean requiresTool) {
        this.requiresTool = requiresTool;
        return this;
    }

    public BlockScriptBuilder soundType(String soundType) {
        this.soundType = soundType != null ? soundType : "stone";
        return this;
    }

    // Stub rendering properties — these will be wired to BlockProperties once rendering is added
    public BlockScriptBuilder model(String path) { return this; }
    public BlockScriptBuilder texture(String path) { return this; }
    public BlockScriptBuilder renderType(String type) { return this; }
    public BlockScriptBuilder color(double r, double g, double b) { return this; }
    public BlockScriptBuilder opacity(double value) { return this; }
    public BlockScriptBuilder tintKey(boolean tintKey) { return this; }
    public BlockScriptBuilder drops(Object drops) { return this; }
    public BlockScriptBuilder property(String key, Object value) { return this; }
    public BlockScriptBuilder states(Object states) { return this; }
    public BlockScriptBuilder variants(Object variants) { return this; }

    Block build() {
        return Block.builder()
                .displayName(displayName)
                .hardness(hardness)
                .resistance(resistance)
                .solid(solid)
                .transparent(transparent)
                .requiresTool(requiresTool)
                .material(material)
                .soundType(soundType)
                .build();
    }
}
