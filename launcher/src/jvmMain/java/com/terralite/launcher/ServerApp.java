package com.terralite.launcher;

import com.terralite.core.logging.Loggers;
import com.terralite.engine.world.World;
import com.terralite.game.content.GameContentLoadReport;
import com.terralite.game.content.GameContentLoader;
import com.terralite.runtime.world.RuntimeWorldFactory;
import com.terralite.server.TerraliteServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

final class ServerApp {
    private static final Logger log = Loggers.get(ServerApp.class);
    private static final Duration TICK_SLEEP = Duration.ofMillis(16);

    private final Path packsRoot;

    ServerApp(Path packsRoot) {
        this.packsRoot = Objects.requireNonNull(packsRoot, "packsRoot");
    }

    void run() throws Exception {
        GameContentLoadReport content = new GameContentLoader().load(packsRoot);
        log.info("Loaded {} content pack(s)", content.packs().size());

        World world = new RuntimeWorldFactory().create(content.gameData());
        TerraliteServer server = TerraliteServer.builder()
                .world(world)
                .contentPacks(content.packs())
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            server.stop();
        }));

        server.start();
        log.info("Server running. Press Ctrl+C to stop.");

        long last = System.nanoTime();
        while (true) {
            Thread.sleep(TICK_SLEEP.toMillis());
            long now = System.nanoTime();
            server.advance(Duration.ofNanos(now - last));
            last = now;
        }
    }
}
