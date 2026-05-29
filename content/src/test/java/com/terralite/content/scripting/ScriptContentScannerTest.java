package com.terralite.content.scripting;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptContentScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void scansScriptsByScope() throws Exception {
        Path packRoot = tempDir.resolve("base");
        Files.createDirectories(packRoot.resolve("scripts/startup"));
        Files.createDirectories(packRoot.resolve("scripts/server"));
        Files.createDirectories(packRoot.resolve("scripts/client/nested"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:base",
              "name": "Base",
              "version": "1.0.0"
            }
            """);
        Files.writeString(packRoot.resolve("scripts/startup/register.js"), "");
        Files.writeString(packRoot.resolve("scripts/server/events.js"), "");
        Files.writeString(packRoot.resolve("scripts/client/nested/hud.js"), "");
        Files.writeString(packRoot.resolve("scripts/client/readme.txt"), "ignored");

        ContentPack pack = new ContentPackLoader().load(packRoot);
        List<ScriptContentFile> scripts = new ScriptContentScanner().scan(pack);

        assertEquals(3, scripts.size());
        assertEquals(ScriptScope.CLIENT, scripts.get(0).scope());
        assertEquals(ScriptScope.SERVER, scripts.get(1).scope());
        assertEquals(ScriptScope.STARTUP, scripts.get(2).scope());
    }
}
