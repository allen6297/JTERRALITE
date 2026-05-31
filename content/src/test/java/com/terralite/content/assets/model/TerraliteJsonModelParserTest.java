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
    void rejectsUnknownTypes() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parser.parse(new StringReader("""
                { "type": "not_real" }
                """)));

        assertTrue(exception.getMessage().contains("supported: cube_all, cube_column, cross"));
    }

    @Test
    void rejectsMissingType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> parser.parse(new StringReader("""
                {}
                """)));

        assertTrue(exception.getMessage().contains("non-blank string type"));
    }
}
