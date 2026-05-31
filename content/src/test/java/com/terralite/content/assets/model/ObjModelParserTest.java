package com.terralite.content.assets.model;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjModelParserTest {
    private final ObjModelParser parser = new ObjModelParser();

    @Test
    void parsesPositionsUvsAndTriangleFaces() throws Exception {
        ContentModelMesh mesh = parser.parse(new StringReader("""
            # tiny triangle
            v 0.0 0.0 0.0
            v 1.0 0.0 0.0
            v 0.0 1.0 0.0
            vt 0.0 0.0
            vt 1.0 0.0
            vt 0.0 1.0
            f 1/1 2/2 3/3
            """));

        assertEquals(3, mesh.vertices().size());
        assertEquals(1, mesh.triangleCount());
        assertEquals(new ContentModelVertex(0.0f, 0.0f, 0.0f, 0.0f, 0.0f), mesh.vertices().get(0));
        assertEquals(new ContentModelVertex(1.0f, 0.0f, 0.0f, 1.0f, 0.0f), mesh.vertices().get(1));
        assertEquals(new ContentModelVertex(0.0f, 1.0f, 0.0f, 0.0f, 1.0f), mesh.vertices().get(2));
    }

    @Test
    void supportsNegativeObjIndices() throws Exception {
        ContentModelMesh mesh = parser.parse(new StringReader("""
            v 0 0 0
            v 1 0 0
            v 0 1 0
            vt 0 0
            vt 1 0
            vt 0 1
            f -3/-3 -2/-2 -1/-1
            """));

        assertEquals(1, mesh.triangleCount());
        assertEquals(new ContentModelVertex(0.0f, 1.0f, 0.0f, 0.0f, 1.0f), mesh.vertices().get(2));
    }

    @Test
    void rejectsNonTriangularFacesForNow() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(new StringReader("""
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            f 1 2 3 4
            """)));
    }
}
