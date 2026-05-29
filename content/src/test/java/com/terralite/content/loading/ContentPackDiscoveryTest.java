package com.terralite.content.loading;

import com.terralite.content.pack.ContentPack;
import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentPackDiscoveryTest {
    @TempDir
    Path tempDir;

    @Test
    void discoversPackDirectoriesInStableNameOrder() throws Exception {
        writePack("z_pack", "terralite:z_pack");
        writePack("a_pack", "terralite:a_pack");
        Files.createDirectories(tempDir.resolve("not_a_pack"));

        List<ContentPack> packs = new ContentPackDiscovery().discover(tempDir);

        assertEquals(List.of(
                ResourceId.id("terralite:a_pack"),
                ResourceId.id("terralite:z_pack")
        ), packs.stream().map(pack -> pack.manifest().id()).toList());
    }

    private void writePack(String directory, String id) throws Exception {
        Path packRoot = tempDir.resolve(directory);
        Files.createDirectories(packRoot);
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "%s",
              "name": "%s",
              "version": "1.0.0"
            }
            """.formatted(id, directory));
    }
}
