package com.terralite.server;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.content.scripting.ScriptScope;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TerraliteServerTest {
    @TempDir
    Path tempDir;

    @Test
    void serverStartsAdvancesAndStopsAuthoritativeEngine() {
        TerraliteServer server = TerraliteServer.builder()
            .config(new ServerConfig(Duration.ofMillis(10), 4))
            .addSimulationSystem(tick -> {
            })
            .build();

        assertEquals(ServerState.CREATED, server.state());

        server.start();

        assertEquals(ServerState.RUNNING, server.state());
        assertEquals(3, server.advance(Duration.ofMillis(35)));

        server.stop();

        assertEquals(ServerState.STOPPED, server.state());
        assertEquals(com.terralite.engine.game.EngineState.STOPPED, server.engine().state());
    }

    @Test
    void serverRejectsAdvanceBeforeStart() {
        TerraliteServer server = TerraliteServer.builder().build();

        assertThrows(IllegalStateException.class, () -> server.advance(Duration.ofMillis(50)));
    }

    @Test
    void serverCannotRestartAfterStop() {
        TerraliteServer server = TerraliteServer.builder().build();

        server.start();
        server.stop();

        assertThrows(IllegalStateException.class, server::start);
    }

    @Test
    void serverOwnsConfiguredWorldState() {
        World world = new World();
        ChunkPos pos = ChunkPos.of(0, 0, 0);

        TerraliteServer server = TerraliteServer.builder()
            .config(new ServerConfig(Duration.ofMillis(10), 5))
            .world(world)
            .addWorldSimulationSystem((tickWorld, tick) -> tickWorld.putChunk(new Chunk(pos)))
            .build();

        server.start();
        assertEquals(1, server.advance(Duration.ofMillis(10)));

        assertSame(world, server.world());
        assertEquals(new Chunk(pos), world.requireChunk(pos));
    }

    @Test
    void serverRunsServerScriptsOnStart() throws Exception {
        ContentPack pack = writePack("base", "api.info('server start');");

        TerraliteServer server = TerraliteServer.builder()
            .contentPacks(List.of(pack))
            .build();

        server.start();

        assertEquals(1, server.serverScripts().executedScripts());
        assertEquals(ScriptScope.SERVER, server.serverScripts().messages().get(0).scope());
        assertEquals("server start", server.serverScripts().messages().get(0).message());
    }

    @Test
    void serverRunsScriptTickHandlersDuringAdvance() throws Exception {
        ContentPack pack = writePack("base", """
            api.onTick(function(tick) {
              api.info('server tick ' + tick.index);
            });
            """);

        TerraliteServer server = TerraliteServer.builder()
            .config(new ServerConfig(Duration.ofMillis(10), 5))
            .contentPacks(List.of(pack))
            .build();

        server.start();
        assertEquals(3, server.advance(Duration.ofMillis(35)));

        assertEquals(3, server.serverScripts().messages().size());
        assertEquals("server tick 1", server.serverScripts().messages().get(0).message());
        assertEquals("server tick 2", server.serverScripts().messages().get(1).message());
        assertEquals("server tick 3", server.serverScripts().messages().get(2).message());
    }

    @Test
    void serverScriptsCanObserveAuthoritativeWorld() throws Exception {
        World world = new World();
        world.entities().create();
        world.entities().create();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        ContentPack pack = writePack("base", """
            api.onTick(function(tick) {
              api.info('entities=' + api.world().entityCount());
              api.info('chunks=' + api.world().chunkCount());
              api.info('spawn=' + api.world().hasChunk(0, 0, 0));
            });
            """);

        TerraliteServer server = TerraliteServer.builder()
            .config(new ServerConfig(Duration.ofMillis(10), 5))
            .world(world)
            .contentPacks(List.of(pack))
            .build();

        server.start();
        assertEquals(1, server.advance(Duration.ofMillis(10)));

        assertEquals("entities=2", server.serverScripts().messages().get(0).message());
        assertEquals("chunks=1", server.serverScripts().messages().get(1).message());
        assertEquals("spawn=true", server.serverScripts().messages().get(2).message());
    }

    @Test
    void serverScriptsCanLoadAndUnloadChunksAuthoritatively() throws Exception {
        World world = new World();
        ContentPack pack = writePack("base", """
            api.onTick(function(tick) {
              if (tick.index == 1) {
                api.info('loaded=' + api.world().loadChunk(2, 0, 3));
                api.info('loadedAgain=' + api.world().loadChunk(2, 0, 3));
              }
              if (tick.index == 2) {
                api.info('unloaded=' + api.world().unloadChunk(2, 0, 3));
                api.info('unloadedAgain=' + api.world().unloadChunk(2, 0, 3));
              }
            });
            """);

        TerraliteServer server = TerraliteServer.builder()
            .config(new ServerConfig(Duration.ofMillis(10), 5))
            .world(world)
            .contentPacks(List.of(pack))
            .build();

        server.start();
        assertEquals(2, server.advance(Duration.ofMillis(20)));

        assertEquals("loaded=true", server.serverScripts().messages().get(0).message());
        assertEquals("loadedAgain=false", server.serverScripts().messages().get(1).message());
        assertEquals("unloaded=true", server.serverScripts().messages().get(2).message());
        assertEquals("unloadedAgain=false", server.serverScripts().messages().get(3).message());
        assertEquals(0, world.chunks().size());
    }

    @Test
    void configRejectsInvalidTickSettings() {
        assertThrows(IllegalArgumentException.class, () -> new ServerConfig(Duration.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> new ServerConfig(Duration.ofMillis(50), 0));
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
