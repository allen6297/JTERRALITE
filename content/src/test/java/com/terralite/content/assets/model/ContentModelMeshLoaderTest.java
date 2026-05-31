package com.terralite.content.assets.model;

import com.terralite.content.assets.ContentModelFormat;
import com.terralite.content.assets.ContentModelAsset;
import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentModelMeshLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void loadsObjModelMeshFromAsset() throws Exception {
        Path path = tempDir.resolve("triangle.obj");
        Files.writeString(path, """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            vt 0 0
            vt 1 0
            vt 0 1
            f 1/1 2/2 3/3
            """);

        ContentModelMesh mesh = new ContentModelMeshLoader().load(new ContentModelAsset(
                ResourceId.id("terralite:block/triangle"),
                ContentModelFormat.WAVEFRONT_OBJ,
                path
        ));

        assertEquals(3, mesh.vertices().size());
    }

    @Test
    void loadsTerraliteJsonModelMeshFromAsset() throws Exception {
        Path path = tempDir.resolve("cube_all.json");
        Files.writeString(path, """
            { "type": "cube_all" }
            """);
        ContentModelAsset model = new ContentModelAsset(
                ResourceId.id("terralite:block/cube_all"),
                ContentModelFormat.TERRALITE_JSON,
                path
        );

        ContentModelMesh mesh = new ContentModelMeshLoader().load(model);

        assertEquals(12, mesh.triangleCount());
    }

    @Test
    void loadsBlockbenchModelMeshFromAsset() throws Exception {
        Path path = tempDir.resolve("cube.bbmodel");
        Files.writeString(path, """
            {
              "elements": [
                {
                  "from": [0, 0, 0],
                  "to": [16, 16, 16],
                  "faces": {
                    "north": {},
                    "south": {},
                    "east": {},
                    "west": {},
                    "up": {},
                    "down": {}
                  }
                }
              ]
            }
            """);

        ContentModelMesh mesh = new ContentModelMeshLoader().load(new ContentModelAsset(
                ResourceId.id("terralite:block/cube"),
                ContentModelFormat.BLOCKBENCH,
                path
        ));

        assertEquals(12, mesh.triangleCount());
    }
}
