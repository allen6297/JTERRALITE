package com.terralite.content.assets.model;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockbenchModelParserTest {
    private final BlockbenchModelParser parser = new BlockbenchModelParser();

    @Test
    void parsesOneCubeElementIntoTriangulatedFaces() throws Exception {
        ContentModelMesh mesh = parser.parse(new StringReader("""
            {
              "elements": [
                {
                  "from": [0, 0, 0],
                  "to": [16, 16, 16],
                  "faces": {
                    "north": { "uv": [0, 0, 16, 16] },
                    "south": { "uv": [0, 0, 16, 16] },
                    "east": { "uv": [0, 0, 16, 16] },
                    "west": { "uv": [0, 0, 16, 16] },
                    "up": { "uv": [0, 0, 16, 16] },
                    "down": { "uv": [0, 0, 16, 16] }
                  }
                }
              ]
            }
            """));

        assertEquals(36, mesh.vertices().size());
        assertEquals(12, mesh.triangleCount());
        assertEquals(new ContentModelVertex(0.0f, 0.0f, 0.0f, 0.0f, 0.0f), mesh.vertices().get(30));
        assertEquals(new ContentModelVertex(1.0f, 1.0f, 0.0f, 1.0f, 1.0f), mesh.vertices().get(32));
    }

    @Test
    void rejectsModelsWithoutElements() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new StringReader("{}")));
    }
}
