package com.terralite.tools.scripting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TypeScriptApiGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void writesStartupScriptDeclarations() throws Exception {
        Path output = tempDir.resolve("terralite-scripting.d.ts");

        TypeScriptApiGenerator.generate(output);

        String declarations = Files.readString(output);
        assertTrue(declarations.contains("registry(type: 'creative_category'"));
        assertTrue(declarations.contains("tag(id: ResourceId): this;"));
        assertTrue(declarations.contains("interface ServerWorldApi"));
    }

    @Test
    void checkPassesWhenDeclarationsMatch() throws Exception {
        Path output = tempDir.resolve("terralite-scripting.d.ts");
        TypeScriptApiGenerator.generate(output);

        TypeScriptApiGenerator.check(output);
    }

    @Test
    void rejectsOutputOutsideWorkingDirectory() {
        assertThrows(IllegalArgumentException.class,
                () -> TypeScriptApiGenerator.resolveInsideWorkingDirectory("../outside.d.ts", tempDir));
    }
}
