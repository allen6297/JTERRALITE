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

class ServerScriptRunnerTest {
    @TempDir
    Path tempDir;

    @Test
    void runsServerScriptsOnly() throws Exception {
        ContentPack pack = writePack("base", "api.info('startup ignored');", "api.info('server ran');");

        ScriptExecutionReport report = new ServerScriptRunner().run(List.of(pack));

        assertEquals(1, report.executedScripts());
        assertEquals(ScriptScope.SERVER, report.messages().get(0).scope());
        assertEquals("server ran", report.messages().get(0).message());
    }

    @Test
    void wrapsScriptFailures() throws Exception {
        ContentPack pack = writePack("broken", "", "throw new Error('broken server');");

        assertThrows(ScriptExecutionException.class, () -> new ServerScriptRunner().run(List.of(pack)));
    }

    private ContentPack writePack(String directory, String startupScript, String serverScript) throws Exception {
        Path packRoot = tempDir.resolve(directory);
        Files.createDirectories(packRoot.resolve("scripts/startup"));
        Files.createDirectories(packRoot.resolve("scripts/server"));
        Files.createDirectories(packRoot.resolve("scripts/client"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:%s",
              "name": "%s",
              "version": "1.0.0"
            }
            """.formatted(directory, directory));
        Files.writeString(packRoot.resolve("scripts/startup/main.js"), startupScript);
        Files.writeString(packRoot.resolve("scripts/server/main.js"), serverScript);
        Files.writeString(packRoot.resolve("scripts/client/main.js"), "api.info('client ignored');");
        return new ContentPackLoader().load(packRoot);
    }
}
