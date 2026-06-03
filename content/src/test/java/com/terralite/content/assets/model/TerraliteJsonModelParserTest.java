package com.terralite.content.assets.model;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerraliteJsonModelParserTest {
    private final TerraliteJsonModelParser parser = new TerraliteJsonModelParser();

    @Test
    void parsesCubeAllModel() throws Exception {
        ContentModelMesh mesh = parser.parse(new StringReader("""
                { "type": "cube_all" }
                """));

        assertEquals(12, mesh.triangleCount());
        assertTrue(mesh.vertices().stream().allMatch(vertex -> vertex.textureSlot().equals("all")));
    }

    @Test
    void parsesCubeColumnTextureSlots() throws Exception {
        ContentModelMesh mesh = parser.parse(new StringReader("""
                { "type": "cube_column" }
                """));

        assertEquals(12, mesh.triangleCount());
        assertEquals(6, mesh.vertices().stream().filter(vertex -> vertex.textureSlot().equals("top")).count());
        assertEquals(6, mesh.vertices().stream().filter(vertex -> vertex.textureSlot().equals("bottom")).count());
        assertEquals(24, mesh.vertices().stream().filter(vertex -> vertex.textureSlot().equals("side")).count());
    }

    @Test
    void parsesDoubleSidedCrossModel() throws Exception {
        ContentModelMesh mesh = parser.parse(new StringReader("""
                { "type": "cross" }
                """));

        assertEquals(8, mesh.triangleCount());
        assertTrue(mesh.vertices().stream().allMatch(vertex -> vertex.textureSlot().equals("all")));
    }

    @Test
    void parsesSizedCrossModel() throws Exception {
        ContentModelMesh mesh = parser.parse(new StringReader("""
                { "type": "cross", "width": 0.5, "height": 1.5 }
                """));

        assertEquals(8, mesh.triangleCount());
        assertEquals(0.25f, mesh.minX());
        assertEquals(0.75f, mesh.maxX());
        assertEquals(1.5f, mesh.maxY());
    }

    @Test
    void parsesElementModelWithTextureSlotsAndUvs() throws Exception {
        ContentModelMesh mesh = parser.parse(new StringReader("""
                {
                  "type": "elements",
                  "elements": [
                    {
                      "from": [0.25, 0.0, 0.25],
                      "to": [0.75, 1.0, 0.75],
                      "faces": {
                        "north": { "texture": "#side", "uv": [0.25, 0.0, 0.75, 1.0] },
                        "up": { "texture": "top" }
                      }
                    }
                  ]
                }
                """));

        assertEquals(4, mesh.triangleCount());
        assertEquals(6, mesh.vertices().stream().filter(vertex -> vertex.textureSlot().equals("side")).count());
        assertEquals(6, mesh.vertices().stream().filter(vertex -> vertex.textureSlot().equals("top")).count());
        assertTrue(mesh.vertices().contains(new ContentModelVertex(0.25f, 0.0f, 0.25f, 0.25f, 0.0f, "side")));
        assertTrue(mesh.vertices().contains(new ContentModelVertex(0.75f, 1.0f, 0.25f, 0.75f, 1.0f, "side")));
    }

    @Test
    void rejectsUnknownTypes() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parser.parse(new StringReader("""
                { "type": "not_real" }
                """)));

        assertTrue(exception.getMessage().contains("supported: cube_all, cube_column, cross, elements"));
    }

    @Test
    void rejectsMissingType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parser.parse(new StringReader("""
                {}
                """)));

        assertTrue(exception.getMessage().contains("non-blank string type"));
    }

    @Test
    void rejectsElementModelsWithoutElementsArray() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parser.parse(new StringReader("""
                { "type": "elements" }
                """)));

        assertTrue(exception.getMessage().contains("elements array"));
    }
}
