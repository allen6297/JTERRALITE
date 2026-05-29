package com.terralite.content.validation;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PackDependencyValidatorTest {
    @TempDir
    Path tempDir;

    @Test
    void reportsMissingRequiredDependencies() throws Exception {
        ContentPack pack = writePack("addon", """
            {
              "id": "terralite:addon",
              "name": "Addon",
              "version": "1.0.0",
              "dependencies": [
                { "id": "terralite:base" },
                { "id": "terralite:optional_theme", "optional": true }
              ]
            }
            """);

        ContentValidationResult result = new PackDependencyValidator().validate(List.of(pack));

        assertFalse(result.isValid());
        assertEquals(1, result.issues().size());
        assertEquals("pack.dependency.missing", result.issues().get(0).code());
        assertThrows(ContentValidationException.class, result::requireValid);
    }

    @Test
    void reportsDuplicatePackIds() throws Exception {
        ContentPack first = writePack("first", manifest("terralite:base", "Base"));
        ContentPack second = writePack("second", manifest("terralite:base", "Base Copy"));

        ContentValidationResult result = new PackDependencyValidator().validate(List.of(first, second));

        assertFalse(result.isValid());
        assertEquals("pack.duplicate", result.issues().get(0).code());
    }

    @Test
    void reportsUnsupportedPackFormatVersion() throws Exception {
        ContentPack pack = writePack("future", """
            {
              "id": "terralite:future",
              "name": "Future",
              "formatVersion": 999,
              "version": "1.0.0"
            }
            """);

        ContentValidationResult result = new PackDependencyValidator().validate(List.of(pack));

        assertFalse(result.isValid());
        assertEquals("pack.format.unsupported", result.issues().get(0).code());
    }

    private ContentPack writePack(String directory, String manifestJson) throws Exception {
        Path packRoot = tempDir.resolve(directory);
        Files.createDirectories(packRoot);
        Files.writeString(packRoot.resolve("pack.json"), manifestJson);
        return new ContentPackLoader().load(packRoot);
    }

    private static String manifest(String id, String name) {
        return """
            {
              "id": "%s",
              "name": "%s",
              "version": "1.0.0"
            }
            """.formatted(id, name);
    }
}
