package com.terralite.game.content;

import com.terralite.content.loading.ContentPackDiscovery;
import com.terralite.content.loading.PackLoadOrderResolver;
import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.GameData;
import com.terralite.core.registry.RegistryManager;
import com.terralite.game.registry.TerraliteRegistries;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class GameContentLoader {
    private final ContentPackDiscovery discovery;
    private final PackLoadOrderResolver orderResolver;
    private final GameContentPackApplier applier;
    private final GameContentValidator validator;

    public GameContentLoader() {
        this(
                new ContentPackDiscovery(),
                new PackLoadOrderResolver(),
                new GameContentPackApplier(),
                new GameContentValidator()
        );
    }

    public GameContentLoader(
            ContentPackDiscovery discovery,
            PackLoadOrderResolver orderResolver,
            GameContentPackApplier applier,
            GameContentValidator validator
    ) {
        this.discovery = Objects.requireNonNull(discovery, "discovery");
        this.orderResolver = Objects.requireNonNull(orderResolver, "orderResolver");
        this.applier = Objects.requireNonNull(applier, "applier");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public GameContentLoadReport load(Path packsRoot) throws IOException {
        Objects.requireNonNull(packsRoot, "packsRoot");
        return load(discovery.discover(packsRoot));
    }

    public GameContentLoadReport load(List<ContentPack> packs) throws IOException {
        Objects.requireNonNull(packs, "packs");

        List<ContentPack> orderedPacks = orderResolver.resolve(packs);
        RegistryManager registries = new RegistryManager();
        createGameRegistries(registries);

        GameContentLoadResult loadResult = applier.apply(orderedPacks, registries);
        GameData gameData = registries.freeze();
        validator.validate(gameData).requireValid();

        return new GameContentLoadReport(orderedPacks, loadResult, gameData);
    }

    private static void createGameRegistries(RegistryManager registries) {
        registries.create(TerraliteRegistries.BLOCKS);
        registries.create(TerraliteRegistries.ITEMS);
        registries.create(TerraliteRegistries.CREATIVE_CATEGORIES);
    }
}
