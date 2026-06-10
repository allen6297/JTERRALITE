package com.terralite.launcher.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.terralite.engine.camera.Camera;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.physics.Velocity;
import com.terralite.engine.save.WorldSnapshotJsonCodec;
import com.terralite.engine.save.WorldSnapshotter;
import com.terralite.engine.world.World;
import com.terralite.launcher.platform.TerraliteDataDir;
import com.terralite.runtime.render.RenderPipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Saves and loads the full game state (world blocks + player transform + camera orientation).
 *
 * <p>Two files are written to {@code saves/}:
 * <ul>
 *   <li>{@code world.json} — all chunks and block states via {@link WorldSnapshotJsonCodec}</li>
 *   <li>{@code player.json} — player position and camera yaw/pitch</li>
 * </ul>
 *
 * <p>Ctrl+S saves, Ctrl+L loads. Loading restores blocks in-place so the existing
 * {@link World} reference (held by the engine and renderer) stays valid.
 */
public final class GameSaveSystem {
    private static final Path SAVE_DIR    = TerraliteDataDir.resolve().resolve("saves");
    private static final Path WORLD_FILE  = SAVE_DIR.resolve("world.json");
    private static final Path PLAYER_FILE = SAVE_DIR.resolve("player.json");

    private final World world;
    private final EntityId playerId;
    private final Camera camera;
    private final RenderPipeline pipeline;
    private final WorldSnapshotter snapshotter = new WorldSnapshotter();
    private final WorldSnapshotJsonCodec worldCodec = new WorldSnapshotJsonCodec();
    private final ObjectMapper mapper = new ObjectMapper()
            .findAndRegisterModules()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private Runnable onLoad = () -> {};

    public GameSaveSystem(World world, EntityId playerId, Camera camera, RenderPipeline pipeline) {
        this.world    = Objects.requireNonNull(world, "world");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.camera   = Objects.requireNonNull(camera, "camera");
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
    }

    public GameSaveSystem onLoad(Runnable callback) {
        this.onLoad = Objects.requireNonNull(callback, "callback");
        return this;
    }

    public void save() {
        try {
            Files.createDirectories(SAVE_DIR);

            worldCodec.write(snapshotter.snapshot(world), WORLD_FILE);

            Transform t = world.entities().require(playerId).get(PhysicsComponents.TRANSFORM);
            if (t == null) t = Transform.ORIGIN;
            mapper.writeValue(PLAYER_FILE.toFile(),
                    new PlayerJson(t.x(), t.y(), t.z(), camera.yaw(), camera.pitch()));

            System.out.println("[Save] Saved to " + SAVE_DIR.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Save] Failed: " + e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(WORLD_FILE)) {
            System.out.println("[Load] No save found at " + SAVE_DIR.toAbsolutePath());
            return;
        }
        try {
            var snapshot = worldCodec.read(WORLD_FILE);

            // Restore world in-place (engine keeps its World reference)
            List.copyOf(world.blocks().positions()).forEach(world::removeBlock);
            List.copyOf(world.chunkPositions()).forEach(world::removeChunk);
            snapshot.chunks().forEach(pos -> world.putChunk(new Chunk(pos)));
            snapshot.blocks().forEach(b -> world.setBlock(b.pos(), b.state()));

            // Restore player
            if (Files.exists(PLAYER_FILE)) {
                PlayerJson p = mapper.readValue(PLAYER_FILE.toFile(), PlayerJson.class);
                Entity player = world.entities().require(playerId);
                player.set(PhysicsComponents.TRANSFORM, new Transform(p.x(), p.y(), p.z()));
                player.set(PhysicsComponents.VELOCITY, Velocity.ZERO);
                camera.setYaw(p.yaw());
                camera.setPitch(p.pitch());
            }

            pipeline.clearMeshCache();
            onLoad.run();
            System.out.println("[Load] Loaded from " + SAVE_DIR.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Load] Failed: " + e.getMessage());
        }
    }

    public boolean hasSave() {
        return Files.exists(WORLD_FILE);
    }

    public record PlayerJson(double x, double y, double z, double yaw, double pitch) {
    }
}
