package com.terralite.content.loading;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.content.validation.ContentValidationException;
import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PackLoadOrderResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void ordersDependenciesBeforeDependents() throws Exception {
        ContentPack addon = writePack("addon", """
            {
              "id": "terralite:addon",
              "name": "Addon",
              "version": "1.0.0",
              "dependencies": [{ "id": "terralite:base" }]
            }
            """);
        ContentPack base = writePack("base", manifest("terralite:base", "Base"));

        List<ContentPack> ordered = new PackLoadOrderResolver().resolve(List.of(addon, base));

        assertEquals(List.of(
                ResourceId.id("terralite:base"),
                ResourceId.id("terralite:addon")
        ), ordered.stream().map(pack -> pack.manifest().id()).toList());
    }

    @Test
    void rejectsDependencyCycles() throws Exception {
        ContentPack first = writePack("first", """
            {
              "id": "terralite:first",
              "name": "First",
              "version": "1.0.0",
              "dependencies": [{ "id": "terralite:second" }]
            }
            """);
        ContentPack second = writePack("second", """
            {
              "id": "terralite:second",
              "name": "Second",
              "version": "1.0.0",
              "dependencies": [{ "id": "terralite:first" }]
            }
            """);

        ContentValidationException exception = assertThrows(
                ContentValidationException.class,
                () -> new PackLoadOrderResolver().resolve(List.of(first, second))
        );

        assertEquals("pack.dependency.cycle", exception.result().issues().get(0).code());
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
