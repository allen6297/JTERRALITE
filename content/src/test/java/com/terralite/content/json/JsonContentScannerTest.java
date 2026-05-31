package com.terralite.content.json;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonContentScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void scansDataAndAssetJsonFiles() throws Exception {
        Path packRoot = tempDir.resolve("base");
        Files.createDirectories(packRoot.resolve("data/blocks/natural"));
        Files.createDirectories(packRoot.resolve("assets/lang"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:base",
              "name": "Terralite Base",
              "version": "1.0.0"
            }
            """);
        Files.writeString(packRoot.resolve("data/blocks/natural/stone.json"), "{}");
        Files.writeString(packRoot.resolve("assets/lang/en_us.json"), "{}");
        Files.writeString(packRoot.resolve("data/blocks/readme.txt"), "ignored");

        ContentPack pack = new ContentPackLoader().load(packRoot);
        List<JsonContentFile> files = new JsonContentScanner().scan(pack);

        assertEquals(2, files.size());
        assertEquals(new JsonContentFile(
                JsonContentRoot.ASSETS,
                "lang",
                ResourceId.id("terralite:en_us"),
                packRoot.resolve("assets/lang/en_us.json")
        ), files.get(0));
        assertEquals(new JsonContentFile(
                JsonContentRoot.DATA,
                "blocks",
                ResourceId.id("terralite:natural/stone"),
                packRoot.resolve("data/blocks/natural/stone.json")
        ), files.get(1));
    }
}
