package com.terralite.game.content;

import com.terralite.content.json.JsonContentFile;
import com.terralite.content.json.JsonContentRoot;
import com.terralite.content.json.JsonContentScanner;
import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.MutableRegistry;
import com.terralite.core.registry.RegistryKey;
import com.terralite.core.registry.RegistryManager;
import com.terralite.game.block.Block;
import com.terralite.game.block.json.BlockJsonLoader;
import com.terralite.game.category.CreativeCategory;
import com.terralite.game.category.json.CreativeCategoryJsonLoader;
import com.terralite.game.item.Item;
import com.terralite.game.item.json.ItemJsonLoader;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.game.tag.Tag;
import com.terralite.game.tag.json.TagJsonLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GameContentPackApplier {
    private final JsonContentScanner scanner;
    private final Map<String, GameContentTypeLoader> loaders;

    public GameContentPackApplier() {
        this(new JsonContentScanner(), defaultLoaders());
    }

    public GameContentPackApplier(JsonContentScanner scanner, Map<String, GameContentTypeLoader> loaders) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.loaders = Map.copyOf(Objects.requireNonNull(loaders, "loaders"));
    }

    public GameContentLoadResult apply(ContentPack pack, RegistryManager registries) throws IOException {
        Objects.requireNonNull(pack, "pack");
        return apply(List.of(pack), registries);
    }

    public GameContentLoadResult apply(List<ContentPack> packs, RegistryManager registries) throws IOException {
        Objects.requireNonNull(packs, "packs");
        Objects.requireNonNull(registries, "registries");

        int scannedFiles = 0;
        int loadedFiles = 0;
        List<JsonContentFile> skippedFiles = new ArrayList<>();

        for (ContentPack pack : packs) {
            List<JsonContentFile> files = scanner.scan(pack);
            scannedFiles += files.size();

            for (JsonContentFile file : files) {
                GameContentTypeLoader loader = loaders.get(file.type());
                if (file.root() != JsonContentRoot.DATA || loader == null) {
                    skippedFiles.add(file);
                    continue;
                }

                loader.load(file, registries);
                loadedFiles++;
            }
        }

        return new GameContentLoadResult(scannedFiles, loadedFiles, skippedFiles);
    }

    private static Map<String, GameContentTypeLoader> defaultLoaders() {
        Map<String, GameContentTypeLoader> loaders = new LinkedHashMap<>();
        BlockJsonLoader blockLoader = new BlockJsonLoader();
        ItemJsonLoader itemLoader = new ItemJsonLoader();
        CreativeCategoryJsonLoader categoryLoader = new CreativeCategoryJsonLoader();
        TagJsonLoader tagLoader = new TagJsonLoader();

        loaders.put("blocks", (file, registries) -> {
            MutableRegistry<Block> blocks = requireOrCreate(registries, TerraliteRegistries.BLOCKS);
            try (InputStream input = Files.newInputStream(file.path())) {
                blockLoader.register(file.id(), input, blocks);
            }
        });
        loaders.put("items", (file, registries) -> {
            MutableRegistry<Item> items = requireOrCreate(registries, TerraliteRegistries.ITEMS);
            try (InputStream input = Files.newInputStream(file.path())) {
                itemLoader.register(file.id(), input, items);
            }
        });
        loaders.put("creative_categories", (file, registries) -> {
            MutableRegistry<CreativeCategory> categories =
                    requireOrCreate(registries, TerraliteRegistries.CREATIVE_CATEGORIES);
            try (InputStream input = Files.newInputStream(file.path())) {
                categoryLoader.register(file.id(), input, categories);
            }
        });
        loaders.put("tags", (file, registries) -> {
            MutableRegistry<Tag> tags = requireOrCreate(registries, TerraliteRegistries.TAGS);
            try (InputStream input = Files.newInputStream(file.path())) {
                tagLoader.register(file.id(), input, tags);
            }
        });

        return loaders;
    }

    private static <T> MutableRegistry<T> requireOrCreate(RegistryManager registries, RegistryKey<T> key) {
        try {
            return registries.requireMutable(key);
        } catch (IllegalArgumentException exception) {
            return registries.create(key);
        }
    }
}
