package com.terralite.engine.game;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.camera.CameraTarget;
import com.terralite.engine.camera.FollowCameraSystem;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.input.InputActions;
import com.terralite.engine.input.InputState;
import com.terralite.engine.physics.MovementSystem;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.physics.Velocity;
import com.terralite.engine.physics.Bounds;
import com.terralite.engine.physics.Collider;
import com.terralite.engine.physics.CollisionResponseSystem;
import com.terralite.engine.physics.WorldBoundsSystem;
import com.terralite.engine.player.PlayerComponents;
import com.terralite.engine.player.PlayerControlled;
import com.terralite.engine.player.PlayerInputSystem;
import com.terralite.engine.terrain.ChunkLoadRadius;
import com.terralite.engine.terrain.ChunkLoaderSystem;
import com.terralite.engine.time.GameClock;
import com.terralite.engine.time.GameClockSystem;
import com.terralite.engine.weather.WeatherCycleSystem;
import com.terralite.engine.weather.WeatherState;
import com.terralite.engine.weather.WeatherType;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineTest {
    @Test
    void engineInitializesStartsAdvancesAndStopsSystems() {
        List<String> calls = new ArrayList<>();

        GameEngine engine = GameEngine.builder()
            .tickDelta(Duration.ofMillis(10))
            .addSystem(new RecordingEngineSystem("first", calls))
            .addSystem(new RecordingEngineSystem("second", calls))
            .addSimulationSystem(tick -> calls.add("tick:" + tick.index()))
            .build();

        assertEquals(EngineState.CREATED, engine.state());
        assertEquals(List.of("first:init", "second:init"), calls);

        engine.start();
        assertEquals(EngineState.RUNNING, engine.state());

        assertEquals(2, engine.advance(Duration.ofMillis(25)));
        engine.stop();

        assertEquals(EngineState.STOPPED, engine.state());
        assertEquals(List.of(
            "first:init",
            "second:init",
            "first:start",
            "second:start",
            "tick:1",
            "tick:2",
            "second:stop",
            "first:stop"
        ), calls);
    }

    @Test
    void engineRejectsAdvanceBeforeStart() {
        GameEngine engine = GameEngine.builder().build();

        assertThrows(IllegalStateException.class, () -> engine.advance(Duration.ofMillis(50)));
    }

    @Test
    void engineCannotRestartAfterStop() {
        GameEngine engine = GameEngine.builder().build();

        engine.start();
        engine.stop();

        assertThrows(IllegalStateException.class, engine::start);
    }

    @Test
    void engineContextExposesConfiguredWorld() {
        World world = new World();

        GameEngine engine = GameEngine.builder()
            .world(world)
            .build();

        assertSame(world, engine.context().world());
    }

    @Test
    void engineContextExposesConfiguredInputState() {
        InputState input = new InputState();

        GameEngine engine = GameEngine.builder()
            .input(input)
            .build();

        assertSame(input, engine.context().input());
    }

    @Test
    void worldSimulationSystemsRunAgainstConfiguredWorld() {
        World world = new World();
        ChunkPos pos = ChunkPos.of(1, 0, 2);

        GameEngine engine = GameEngine.builder()
            .world(world)
            .tickDelta(Duration.ofMillis(10))
            .addWorldSimulationSystem((tickWorld, tick) -> tickWorld.putChunk(new Chunk(pos)))
            .build();

        engine.start();
        assertEquals(1, engine.advance(Duration.ofMillis(10)));

        assertSame(world, engine.context().world());
        assertEquals(new Chunk(pos), world.requireChunk(pos));
    }

    @Test
    void worldSimulationSystemsCanUpdateEntityComponents() {
        World world = new World();
        Entity entity = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN)
            .set(PhysicsComponents.VELOCITY, new Velocity(100.0, 0.0, 0.0));

        GameEngine engine = GameEngine.builder()
            .world(world)
            .tickDelta(Duration.ofMillis(10))
            .addWorldSimulationSystem(new MovementSystem())
            .build();

        engine.start();
        assertEquals(3, engine.advance(Duration.ofMillis(30)));

        assertEquals(new Transform(3.0, 0.0, 0.0), entity.require(PhysicsComponents.TRANSFORM));
    }

    @Test
    void playerInputAndMovementSystemsFormInputToTransformLoop() {
        World world = new World();
        InputState input = new InputState();
        Entity player = world.entities().create()
            .set(PlayerComponents.PLAYER_CONTROLLED, new PlayerControlled(100.0))
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);

        input.press(InputActions.MOVE_FORWARD);

        GameEngine engine = GameEngine.builder()
            .world(world)
            .input(input)
            .tickDelta(Duration.ofMillis(10))
            .addWorldSimulationSystem(new PlayerInputSystem(input))
            .addWorldSimulationSystem(new MovementSystem())
            .build();

        engine.start();
        assertEquals(3, engine.advance(Duration.ofMillis(30)));

        assertEquals(new Transform(0.0, 0.0, 3.0), player.require(PhysicsComponents.TRANSFORM));
    }

    @Test
    void cameraCanFollowPlayerMovedByInput() {
        World world = new World();
        InputState input = new InputState();
        Camera camera = new Camera();
        Entity player = world.entities().create()
            .set(PlayerComponents.PLAYER_CONTROLLED, new PlayerControlled(100.0))
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);

        input.press(InputActions.MOVE_RIGHT);

        GameEngine engine = GameEngine.builder()
            .world(world)
            .input(input)
            .tickDelta(Duration.ofMillis(10))
            .addWorldSimulationSystem(new PlayerInputSystem(input))
            .addWorldSimulationSystem(new MovementSystem())
            .addWorldSimulationSystem(new FollowCameraSystem(
                camera,
                new CameraTarget(player.id()),
                new Transform(0.0, 5.0, -10.0)
            ))
            .build();

        engine.start();
        assertEquals(2, engine.advance(Duration.ofMillis(20)));

        assertEquals(new Transform(2.0, 5.0, -10.0), camera.transform());
    }

    @Test
    void chunkLoaderCanTrackPlayerAfterMovement() {
        World world = new World();
        InputState input = new InputState();
        Entity player = world.entities().create()
            .set(PlayerComponents.PLAYER_CONTROLLED, new PlayerControlled(1_600.0))
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN);

        input.press(InputActions.MOVE_RIGHT);

        GameEngine engine = GameEngine.builder()
            .world(world)
            .input(input)
            .tickDelta(Duration.ofMillis(10))
            .addWorldSimulationSystem(new PlayerInputSystem(input))
            .addWorldSimulationSystem(new MovementSystem())
            .addWorldSimulationSystem(new ChunkLoaderSystem(player.id(), 16, ChunkLoadRadius.horizontal(0)))
            .build();

        engine.start();
        assertEquals(1, engine.advance(Duration.ofMillis(10)));

        assertTrue(world.containsChunk(ChunkPos.of(1, 0, 0)));
    }

    @Test
    void simulationSystemsCanAdvanceGlobalClock() {
        GameClock clock = new GameClock(Duration.ofSeconds(1));
        GameEngine engine = GameEngine.builder()
            .tickDelta(Duration.ofMillis(100))
            .addSimulationSystem(new GameClockSystem(clock))
            .build();

        engine.start();
        assertEquals(3, engine.advance(Duration.ofMillis(300)));

        assertEquals(Duration.ofMillis(300), clock.elapsed());
        assertEquals(0.3, clock.dayProgress());
    }

    @Test
    void simulationSystemsCanCycleWeather() {
        WeatherState weather = new WeatherState();
        GameEngine engine = GameEngine.builder()
            .tickDelta(Duration.ofMillis(100))
            .addSimulationSystem(WeatherCycleSystem.defaultCycle(weather, Duration.ofMillis(200)))
            .build();

        engine.start();
        assertEquals(2, engine.advance(Duration.ofMillis(200)));

        assertEquals(WeatherType.RAIN, weather.type());
    }

    @Test
    void boundsSystemCanClampAfterMovement() {
        World world = new World();
        Entity entity = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, new Transform(9.0, 0.0, 0.0))
            .set(PhysicsComponents.VELOCITY, new Velocity(5.0, 0.0, 0.0));

        GameEngine engine = GameEngine.builder()
            .world(world)
            .tickDelta(Duration.ofSeconds(1))
            .addWorldSimulationSystem(new MovementSystem())
            .addWorldSimulationSystem(new WorldBoundsSystem(new Bounds(0.0, 0.0, 0.0, 10.0, 10.0, 10.0)))
            .build();

        engine.start();
        assertEquals(1, engine.advance(Duration.ofSeconds(1)));

        assertEquals(new Transform(10.0, 0.0, 0.0), entity.require(PhysicsComponents.TRANSFORM));
    }

    @Test
    void collisionResponseCanRevertMovementAfterOverlap() {
        World world = new World();
        Entity mover = world.entities().create()
            .set(PhysicsComponents.TRANSFORM, new Transform(-2.0, 0.0, 0.0))
            .set(PhysicsComponents.VELOCITY, new Velocity(2.0, 0.0, 0.0))
            .set(PhysicsComponents.COLLIDER, new Collider(0.5, 0.5, 0.5));
        world.entities().create()
            .set(PhysicsComponents.TRANSFORM, Transform.ORIGIN)
            .set(PhysicsComponents.COLLIDER, new Collider(0.5, 0.5, 0.5));

        GameEngine engine = GameEngine.builder()
            .world(world)
            .tickDelta(Duration.ofSeconds(1))
            .addWorldSimulationSystem(new MovementSystem())
            .addWorldSimulationSystem(new CollisionResponseSystem())
            .build();

        engine.start();
        assertEquals(1, engine.advance(Duration.ofSeconds(1)));

        assertEquals(new Transform(-2.0, 0.0, 0.0), mover.require(PhysicsComponents.TRANSFORM));
        assertEquals(Velocity.ZERO, mover.require(PhysicsComponents.VELOCITY));
    }

    private record RecordingEngineSystem(String name, List<String> calls) implements EngineSystem {
        @Override
        public void initialize(EngineContext context) {
            calls.add(name + ":init");
        }

        @Override
        public void start(EngineContext context) {
            calls.add(name + ":start");
        }

        @Override
        public void stop(EngineContext context) {
            calls.add(name + ":stop");
        }
    }
}
