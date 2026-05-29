package com.terralite.game.content;

import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.GameData;

import java.util.List;
import java.util.Objects;

public record GameContentLoadReport(
        List<ContentPack> packs,
        GameContentLoadResult loadResult,
        GameData gameData
) {
    public GameContentLoadReport {
        packs = List.copyOf(Objects.requireNonNull(packs, "packs"));
        Objects.requireNonNull(loadResult, "loadResult");
        Objects.requireNonNull(gameData, "gameData");
    }
}
