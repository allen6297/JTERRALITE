package com.terralite.content.scripting;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerScriptHostTest {
    @TempDir
    Path tempDir;

    @Test
    void serverScriptsCanRegisterTickHandlers() throws Exception {
        ContentPack pack = writePack("base", """
            api.onTick(function(tick) {
              api.info('tick ' + tick.index + ' at ' + tick.totalMillis);
            });
            """);
        ServerScriptHost host = new ServerScriptHost();

        ScriptExecutionReport loadReport = host.load(List.of(pack));
        host.tick(1, Duration.ofMillis(50), Duration.ofMillis(50));
        host.tick(2, Duration.ofMillis(50), Duration.ofMillis(100));

        assertEquals(1, loadReport.executedScripts());
        assertEquals(2, host.report().messages().size());
        assertEquals("tick 1 at 50", host.report().messages().get(0).message());
        assertEquals("tick 2 at 100", host.report().messages().get(1).message());
    }

    @Test
    void tickHandlerFailuresAreWrapped() throws Exception {
        ContentPack pack = writePack("broken", """
            api.onTick(function(tick) {
              throw new Error('broken tick');
            });
            """);
        ServerScriptHost host = new ServerScriptHost();

        host.load(List.of(pack));

        assertThrows(ScriptExecutionException.class,
                () -> host.tick(1, Duration.ofMillis(50), Duration.ofMillis(50)));
    }

    @Test
    void serverScriptsCanReadWorldState() throws Exception {
        ContentPack pack = writePack("base", """
            api.onTick(function(tick) {
              api.info('entities=' + api.world().entityCount());
              api.info('chunks=' + api.world().chunkCount());
              api.info('spawn=' + api.world().hasChunk(0, 0, 0));
            });
            """);
        ServerWorldScriptApi world = new ServerWorldScriptApi() {
            @Override
            public int entityCount() {
                return 2;
            }

            @Override
            public int chunkCount() {
                return 1;
            }

            @Override
            public boolean hasChunk(int x, int y, int z) {
                return x == 0 && y == 0 && z == 0;
            }

            @Override
            public boolean loadChunk(int x, int y, int z) {
                return false;
            }

            @Override
            public boolean unloadChunk(int x, int y, int z) {
                return false;
            }
        };
        ServerScriptHost host = new ServerScriptHost(new ScriptContentScanner(), world);

        host.load(List.of(pack));
        host.tick(1, Duration.ofMillis(50), Duration.ofMillis(50));

        assertEquals("entities=2", host.report().messages().get(0).message());
        assertEquals("chunks=1", host.report().messages().get(1).message());
        assertEquals("spawn=true", host.report().messages().get(2).message());
    }

    @Test
    void serverScriptsCanRequestChunkLoadAndUnload() throws Exception {
        ContentPack pack = writePack("base", """
            api.onTick(function(tick) {
              api.info('load=' + api.world().loadChunk(1, 0, 2));
              api.info('unload=' + api.world().unloadChunk(1, 0, 2));
            });
            """);
        ServerWorldScriptApi world = new ServerWorldScriptApi() {
            @Override
            public int entityCount() {
                return 0;
            }

            @Override
            public int chunkCount() {
                return 0;
            }

            @Override
            public boolean hasChunk(int x, int y, int z) {
                return false;
            }

            @Override
            public boolean loadChunk(int x, int y, int z) {
                return x == 1 && y == 0 && z == 2;
            }

            @Override
            public boolean unloadChunk(int x, int y, int z) {
                return x == 1 && y == 0 && z == 2;
            }
        };
        ServerScriptHost host = new ServerScriptHost(new ScriptContentScanner(), world);

        host.load(List.of(pack));
        host.tick(1, Duration.ofMillis(50), Duration.ofMillis(50));

        assertEquals("load=true", host.report().messages().get(0).message());
        assertEquals("unload=true", host.report().messages().get(1).message());
    }

    private ContentPack writePack(String directory, String serverScript) throws Exception {
        Path packRoot = tempDir.resolve(directory);
        Files.createDirectories(packRoot.resolve("scripts/server"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:%s",
              "name": "%s",
              "version": "1.0.0"
            }
            """.formatted(directory, directory));
        Files.writeString(packRoot.resolve("scripts/server/main.js"), serverScript);
        return new ContentPackLoader().load(packRoot);
    }
}
