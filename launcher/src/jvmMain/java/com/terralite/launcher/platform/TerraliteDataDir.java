package com.terralite.launcher.platform;

import java.nio.file.Path;

/**
 * Resolves the platform-appropriate application data directory for Terralite.
 *
 * <ul>
 *   <li>Windows  — {@code %APPDATA%\Terralite}</li>
 *   <li>macOS    — {@code ~/Library/Application Support/Terralite}</li>
 *   <li>Linux    — {@code $XDG_DATA_HOME/Terralite} or {@code ~/.local/share/Terralite}</li>
 * </ul>
 */
public final class TerraliteDataDir {
    private static final String APP_NAME = "Terralite";

    private TerraliteDataDir() {
    }

    public static Path resolve() {
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) return Path.of(appData, APP_NAME);
            return fallback();
        }

        if (os.contains("mac")) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", APP_NAME);
        }

        // Linux / other Unix
        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) return Path.of(xdg, APP_NAME);
        return Path.of(System.getProperty("user.home"), ".local", "share", APP_NAME);
    }

    private static Path fallback() {
        return Path.of(System.getProperty("user.home"), APP_NAME);
    }
}
