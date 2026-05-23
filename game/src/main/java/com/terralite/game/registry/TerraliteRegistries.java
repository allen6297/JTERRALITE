package com.terralite.game.registry;

import com.terralite.core.registry.RegistryKey;
import com.terralite.game.block.Block;

public final class TerraliteRegistries {
    public static final RegistryKey<Block> BLOCKS = RegistryKey.of("terralite:blocks", Block.class);

    private TerraliteRegistries() {
    }
}
