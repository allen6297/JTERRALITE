package com.terralite.content.scripting;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StartupScriptRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void runsStartupScriptsInPackOrder() throws Exception {
        ContentPack first = writePack("first", "api.info('first startup');");
        ContentPack second = writePack("second", "api.info('second startup');");

        ScriptExecutionReport report = new StartupScriptRunner().run(List.of(first, second));

        assertEquals(2, report.executedScripts());
        assertEquals("first startup", report.messages().get(0).message());
        assertEquals("second startup", report.messages().get(1).message());
        assertEquals(ScriptScope.STARTUP, report.messages().get(0).scope());
    }

    @Test
    void wrapsScriptFailures() throws Exception {
        ContentPack pack = writePack("broken", "throw new Error('broken startup');");

        assertThrows(ScriptExecutionException.class, () -> new StartupScriptRunner().run(List.of(pack)));
    }

    private ContentPack writePack(String directory, String startupScript) throws Exception {
        Path packRoot = tempDir.resolve(directory);
        Files.createDirectories(packRoot.resolve("scripts/startup"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:%s",
              "name": "%s",
              "version": "1.0.0"
            }
            """.formatted(directory, directory));
        Files.writeString(packRoot.resolve("scripts/startup/main.js"), startupScript);
        return new ContentPackLoader().load(packRoot);
    }
}
