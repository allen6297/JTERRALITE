package com.terralite.game.content;

import com.terralite.content.json.JsonContentFile;
import com.terralite.core.registry.RegistryManager;

import java.io.IOException;

@FunctionalInterface
public interface GameContentTypeLoader {
    void load(JsonContentFile file, RegistryManager registries) throws IOException;
}
