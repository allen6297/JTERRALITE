package com.terralite.content.pack;

import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentPackLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsContentPackFromDirectory() throws Exception {
        Path packRoot = tempDir.resolve("base");
        Files.createDirectories(packRoot);
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:base",
              "name": "Terralite Base",
              "version": "1.0.0"
            }
            """);

        ContentPack pack = new ContentPackLoader().load(packRoot);

        assertEquals(packRoot.toAbsolutePath().normalize(), pack.root());
        assertEquals(ResourceId.id("terralite:base"), pack.manifest().id());
    }

    @Test
    void rejectsDirectoryWithoutManifest() throws Exception {
        Path packRoot = tempDir.resolve("missing_manifest");
        Files.createDirectories(packRoot);

        assertThrows(Exception.class, () -> new ContentPackLoader().load(packRoot));
    }
}
