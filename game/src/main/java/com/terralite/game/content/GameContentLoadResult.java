package com.terralite.game.content;

import com.terralite.content.json.JsonContentFile;

import java.util.List;
import java.util.Objects;

public record GameContentLoadResult(int scannedFiles, int loadedFiles, List<JsonContentFile> skippedFiles) {
    public GameContentLoadResult {
        if (scannedFiles < 0) {
            throw new IllegalArgumentException("Scanned file count cannot be negative");
        }
        if (loadedFiles < 0) {
            throw new IllegalArgumentException("Loaded file count cannot be negative");
        }
        if (loadedFiles > scannedFiles) {
            throw new IllegalArgumentException("Loaded file count cannot exceed scanned file count");
        }
        skippedFiles = List.copyOf(Objects.requireNonNull(skippedFiles, "skippedFiles"));
    }
}
