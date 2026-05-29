package com.terralite.content.manifest;

import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackManifestLoaderTest {
    @Test
    void loadsPackManifestJson() throws Exception {
        String json = """
            {
              "id": "terralite:base",
              "name": "Terralite Base",
              "version": "1.0.0",
              "description": "Built-in content.",
              "dependencies": [
                { "id": "terralite:core", "optional": false },
                { "id": "terralite:extras", "optional": true }
              ]
            }
            """;

        PackManifest manifest = new PackManifestLoader().load(stream(json));

        assertEquals(ResourceId.id("terralite:base"), manifest.id());
        assertEquals("Terralite Base", manifest.name());
        assertEquals("1.0.0", manifest.version());
        assertEquals("Built-in content.", manifest.description());
        assertEquals(List.of(
                PackDependency.required(ResourceId.id("terralite:core")),
                PackDependency.optional(ResourceId.id("terralite:extras"))
        ), manifest.dependencies());
        assertFalse(manifest.dependencies().get(0).optional());
        assertTrue(manifest.dependencies().get(1).optional());
    }

    private static ByteArrayInputStream stream(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
