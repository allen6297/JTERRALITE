package com.terralite.content.assets.model;

import com.terralite.content.assets.ContentModelIndex;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentModelMeshLibraryTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsSupportedObjAndBlockbenchMeshesFromPackModels() throws Exception {
        Path packRoot = tempDir.resolve("base");
        Files.createDirectories(packRoot.resolve("assets/models/block"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:base",
              "name": "Base",
              "version": "1.0.0"
            }
            """);
        Files.writeString(packRoot.resolve("assets/models/block/triangle.obj"), """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f 1 2 3
            """);
        Files.writeString(packRoot.resolve("assets/models/block/cube.bbmodel"), """
            {
              "elements": [
                {
                  "from": [0, 0, 0],
                  "to": [16, 16, 16],
                  "faces": { "north": {}, "south": {}, "east": {}, "west": {}, "up": {}, "down": {} }
                }
              ]
            }
            """);
        Files.writeString(packRoot.resolve("assets/models/block/cube_all.json"), "{}");

        var pack = new ContentPackLoader().load(packRoot);
        var meshes = new ContentModelMeshLibrary().loadSupported(ContentModelIndex.load(List.of(pack)));

        assertEquals(2, meshes.size());
        assertTrue(meshes.containsKey(ResourceId.id("terralite:block/triangle")));
        assertEquals(12, meshes.get(ResourceId.id("terralite:block/cube")).triangleCount());
    }
}
