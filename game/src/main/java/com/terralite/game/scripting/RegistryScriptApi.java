package com.terralite.game.scripting;

import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.ResourceId;
import com.terralite.game.biome.Biome;
import com.terralite.game.block.Block;
import com.terralite.game.block.BlockProperties;
import com.terralite.game.item.Item;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.Objects;

public final class RegistryScriptApi {
    private final MutableRegistry<Block> blockRegistry;
    private final MutableRegistry<Item> itemRegistry;
    private final MutableRegistry<Biome> biomeRegistry;

    public RegistryScriptApi(
            MutableRegistry<Block> blockRegistry,
            MutableRegistry<Item> itemRegistry,
            MutableRegistry<Biome> biomeRegistry
    ) {
        this.blockRegistry = Objects.requireNonNull(blockRegistry, "blockRegistry");
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry");
        this.biomeRegistry = Objects.requireNonNull(biomeRegistry, "biomeRegistry");
    }

    public Block getBlock(String id) {
        return blockRegistry.require(ResourceId.id(id));
    }

    public Item getItem(String id) {
        return itemRegistry.require(ResourceId.id(id));
    }

    public Biome getBiome(String id) {
        return biomeRegistry.require(ResourceId.id(id));
    }

    public void modifyBlock(String id, Function fn) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fn, "fn");

        ResourceId resourceId = ResourceId.id(id);
        Block existing = blockRegistry.require(resourceId);
        BlockModifier modifier = new BlockModifier(existing.properties());

        Context cx = Context.getCurrentContext();
        Scriptable scope = fn.getParentScope();
        fn.call(cx, scope, scope, new Object[]{Context.javaToJS(modifier, scope)});

        blockRegistry.replace(resourceId, modifier.toBlock());
    }

    public static final class BlockModifier {
        private String displayName;
        private float hardness;
        private float resistance;
        private boolean solid;
        private boolean transparent;
        private boolean requiresTool;
        private String material;
        private String soundType;
        private final BlockProperties originalProperties;

        BlockModifier(BlockProperties props) {
            this.originalProperties = Objects.requireNonNull(props, "props");
            this.displayName = props.displayName();
            this.hardness = props.hardness();
            this.resistance = props.resistance();
            this.solid = props.solid();
            this.transparent = props.transparent();
            this.requiresTool = props.requiresTool();
            this.material = props.material();
            this.soundType = props.soundType();
        }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName != null ? displayName : ""; }
        public float getHardness() { return hardness; }
        public void setHardness(float hardness) { this.hardness = hardness; }
        public float getResistance() { return resistance; }
        public void setResistance(float resistance) { this.resistance = resistance; }
        public boolean isSolid() { return solid; }
        public void setSolid(boolean solid) { this.solid = solid; }
        public boolean isTransparent() { return transparent; }
        public void setTransparent(boolean transparent) { this.transparent = transparent; }
        public boolean isRequiresTool() { return requiresTool; }
        public void setRequiresTool(boolean requiresTool) { this.requiresTool = requiresTool; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material != null ? material : "stone"; }
        public String getSoundType() { return soundType; }
        public void setSoundType(String soundType) { this.soundType = soundType != null ? soundType : "stone"; }

        Block toBlock() {
            if (hardness < 0) {
                throw new IllegalArgumentException("hardness must be non-negative: " + hardness);
            }
            if (resistance < 0) {
                throw new IllegalArgumentException("resistance must be non-negative: " + resistance);
            }
            Block.Builder builder = Block.builder()
                    .displayName(displayName)
                    .hardness(hardness)
                    .resistance(resistance)
                    .solid(solid)
                    .transparent(transparent)
                    .requiresTool(requiresTool)
                    .material(material)
                    .soundType(soundType)
                    .categories(originalProperties.categories())
                    .model(originalProperties.model());
            if (originalProperties.textures() != null) {
                builder.textures(originalProperties.textures());
            }
            return builder.build();
        }
    }
}
