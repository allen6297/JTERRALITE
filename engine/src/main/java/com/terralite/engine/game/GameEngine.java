package com.terralite.engine.game;

import com.terralite.core.registry.RegistryManager;
import com.terralite.engine.input.InputState;
import com.terralite.engine.simulation.FixedTimestepSimulation;
import com.terralite.engine.simulation.SimulationSystem;
import com.terralite.engine.simulation.WorldSimulationAdapter;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.world.World;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GameEngine {
    private final EngineContext context;
    private final List<EngineSystem> systems;
    private EngineState state = EngineState.CREATED;

    private GameEngine(EngineContext context, List<EngineSystem> systems) {
        this.context = Objects.requireNonNull(context, "context");
        this.systems = List.copyOf(systems);

        for (EngineSystem system : this.systems) {
            system.initialize(context);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public EngineContext context() {
        return context;
    }

    public EngineState state() {
        return state;
    }

    public void start() {
        if (state == EngineState.RUNNING) {
            return;
        }

        if (state == EngineState.STOPPED) {
            throw new IllegalStateException("Engine cannot be restarted after stop");
        }

        state = EngineState.RUNNING;
        for (EngineSystem system : systems) {
            system.start(context);
        }
    }

    public int advance(Duration elapsed) {
        ensureRunning();
        return context.simulation().advance(elapsed);
    }

    public void stop() {
        if (state != EngineState.RUNNING) {
            return;
        }

        for (int i = systems.size() - 1; i >= 0; i--) {
            systems.get(i).stop(context);
        }
        state = EngineState.STOPPED;
    }

    private void ensureRunning() {
        if (state != EngineState.RUNNING) {
            throw new IllegalStateException("Engine is not running");
        }
    }

    public static final class Builder {
        private final RegistryManager registries = new RegistryManager();
        private final List<EngineSystem> engineSystems = new ArrayList<>();
        private final List<SimulationSystem> simulationSystems = new ArrayList<>();
        private final List<WorldSimulationSystem> worldSimulationSystems = new ArrayList<>();
        private World world = new World();
        private InputState input = new InputState();
        private Duration tickDelta = Duration.ofMillis(50);
        private int maxTicksPerAdvance = 5;

        private Builder() {
        }

        public Builder tickDelta(Duration tickDelta) {
            this.tickDelta = Objects.requireNonNull(tickDelta, "tickDelta");
            return this;
        }

        public Builder maxTicksPerAdvance(int maxTicksPerAdvance) {
            this.maxTicksPerAdvance = maxTicksPerAdvance;
            return this;
        }

        public Builder addSystem(EngineSystem system) {
            engineSystems.add(Objects.requireNonNull(system, "system"));
            return this;
        }

        public Builder addSimulationSystem(SimulationSystem system) {
            simulationSystems.add(Objects.requireNonNull(system, "system"));
            return this;
        }

        public Builder addWorldSimulationSystem(WorldSimulationSystem system) {
            worldSimulationSystems.add(Objects.requireNonNull(system, "system"));
            return this;
        }

        public Builder world(World world) {
            this.world = Objects.requireNonNull(world, "world");
            return this;
        }

        public Builder input(InputState input) {
            this.input = Objects.requireNonNull(input, "input");
            return this;
        }

        public GameEngine build() {
            List<SimulationSystem> allSimulationSystems = new ArrayList<>(simulationSystems);
            for (WorldSimulationSystem system : worldSimulationSystems) {
                allSimulationSystems.add(new WorldSimulationAdapter(world, system));
            }

            FixedTimestepSimulation simulation = FixedTimestepSimulation.builder()
                .tickDelta(tickDelta)
                .maxTicksPerAdvance(maxTicksPerAdvance)
                .addSystems(allSimulationSystems)
                .build();
            return new GameEngine(new EngineContext(registries, world, input, simulation), engineSystems);
        }
    }
}
