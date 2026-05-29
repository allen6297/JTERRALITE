package com.terralite.server;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.scripting.ScriptContentScanner;
import com.terralite.content.scripting.ScriptExecutionReport;
import com.terralite.content.scripting.ServerScriptHost;
import com.terralite.content.scripting.ServerWorldScriptApi;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.game.GameEngine;
import com.terralite.engine.simulation.SimulationSystem;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TerraliteServer {
    private final ServerConfig config;
    private final GameEngine engine;
    private final List<ContentPack> contentPacks;
    private final ServerScriptHost serverScriptHost;
    private ServerState state = ServerState.CREATED;

    private TerraliteServer(
            ServerConfig config,
            GameEngine engine,
            List<ContentPack> contentPacks,
            ServerScriptHost serverScriptHost
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.contentPacks = List.copyOf(Objects.requireNonNull(contentPacks, "contentPacks"));
        this.serverScriptHost = Objects.requireNonNull(serverScriptHost, "serverScriptHost");
    }

    public static Builder builder() {
        return new Builder();
    }

    public ServerConfig config() {
        return config;
    }

    public GameEngine engine() {
        return engine;
    }

    public World world() {
        return engine.context().world();
    }

    public ScriptExecutionReport serverScripts() {
        return serverScriptHost.report();
    }

    public ServerState state() {
        return state;
    }

    public void start() {
        if (state == ServerState.RUNNING) {
            return;
        }

        if (state == ServerState.STOPPED) {
            throw new IllegalStateException("Server cannot be restarted after stop");
        }

        try {
            serverScriptHost.load(contentPacks);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to run server scripts", exception);
        }

        engine.start();
        state = ServerState.RUNNING;
    }

    public int advance(Duration elapsed) {
        ensureRunning();
        return engine.advance(elapsed);
    }

    public void stop() {
        if (state != ServerState.RUNNING) {
            return;
        }

        engine.stop();
        state = ServerState.STOPPED;
    }

    private void ensureRunning() {
        if (state != ServerState.RUNNING) {
            throw new IllegalStateException("Server is not running");
        }
    }

    public static final class Builder {
        private ServerConfig config = ServerConfig.defaults();
        private World world = new World();
        private final ServerWorldScriptApi worldScriptApi = new ServerWorldView();
        private final ServerScriptHost serverScriptHost = new ServerScriptHost(new ScriptContentScanner(), worldScriptApi);
        private final GameEngine.Builder engineBuilder = GameEngine.builder();
        private final List<ContentPack> contentPacks = new ArrayList<>();

        private Builder() {
            applyConfig(config);
            engineBuilder.world(world);
            engineBuilder.addSimulationSystem(tick -> serverScriptHost.tick(tick.index(), tick.delta(), tick.totalTime()));
        }

        public Builder config(ServerConfig config) {
            this.config = Objects.requireNonNull(config, "config");
            applyConfig(this.config);
            return this;
        }

        public Builder world(World world) {
            this.world = Objects.requireNonNull(world, "world");
            engineBuilder.world(world);
            return this;
        }

        public Builder contentPacks(List<ContentPack> contentPacks) {
            this.contentPacks.clear();
            this.contentPacks.addAll(List.copyOf(Objects.requireNonNull(contentPacks, "contentPacks")));
            return this;
        }

        public Builder addSimulationSystem(SimulationSystem system) {
            engineBuilder.addSimulationSystem(system);
            return this;
        }

        public Builder addWorldSimulationSystem(WorldSimulationSystem system) {
            engineBuilder.addWorldSimulationSystem(system);
            return this;
        }

        public TerraliteServer build() {
            applyConfig(config);
            return new TerraliteServer(config, engineBuilder.build(), contentPacks, serverScriptHost);
        }

        private void applyConfig(ServerConfig config) {
            engineBuilder.tickDelta(config.tickDelta());
            engineBuilder.maxTicksPerAdvance(config.maxTicksPerAdvance());
        }

        private final class ServerWorldView implements ServerWorldScriptApi {
            @Override
            public int entityCount() {
                return world.entities().size();
            }

            @Override
            public int chunkCount() {
                return world.chunks().size();
            }

            @Override
            public boolean hasChunk(int x, int y, int z) {
                return world.containsChunk(ChunkPos.of(x, y, z));
            }

            @Override
            public boolean loadChunk(int x, int y, int z) {
                ChunkPos pos = ChunkPos.of(x, y, z);
                if (world.containsChunk(pos)) {
                    return false;
                }

                world.putChunk(new Chunk(pos));
                return true;
            }

            @Override
            public boolean unloadChunk(int x, int y, int z) {
                ChunkPos pos = ChunkPos.of(x, y, z);
                if (!world.containsChunk(pos)) {
                    return false;
                }

                world.removeChunk(pos);
                return true;
            }
        }
    }
}
