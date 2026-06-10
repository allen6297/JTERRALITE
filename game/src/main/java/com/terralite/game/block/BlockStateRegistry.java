package com.terralite.game.block;

import com.terralite.core.registry.GameData;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.terrain.CompactBlockStorage;
import com.terralite.game.registry.TerraliteRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

public final class BlockStateRegistry {
    private final List<BlockState> states;
    private final Map<BlockState, Integer> idsByState;
    private final Map<ResourceId, BlockState> defaultStates;

    private BlockStateRegistry(
            List<BlockState> states,
            Map<BlockState, Integer> idsByState,
            Map<ResourceId, BlockState> defaultStates
    ) {
        this.states = List.copyOf(states);
        this.idsByState = Map.copyOf(idsByState);
        this.defaultStates = Map.copyOf(defaultStates);
    }

    public static BlockStateRegistry from(GameData gameData) {
        Objects.requireNonNull(gameData, "gameData");

        List<BlockState> states = new ArrayList<>();
        Map<BlockState, Integer> idsByState = new LinkedHashMap<>();
        Map<ResourceId, BlockState> defaultStates = new LinkedHashMap<>();
        idsByState.put(BlockState.AIR, states.size());
        states.add(BlockState.AIR);
        for (ResourceId blockId : gameData.registry(TerraliteRegistries.BLOCKS).ids().stream()
                .sorted(Comparator.comparing(ResourceId::toString))
                .toList()) {
            Block block = gameData.registry(TerraliteRegistries.BLOCKS).require(blockId);
            BlockStateDefinition definition = block.properties().stateDefinition();
            List<BlockState> blockStates = statesFor(blockId, definition);
            for (BlockState state : blockStates) {
                idsByState.put(state, states.size());
                states.add(state);
            }
            defaultStates.put(blockId, new BlockState(blockId, definition.defaultValues()));
        }
        return new BlockStateRegistry(states, idsByState, defaultStates);
    }

    public int size() {
        return states.size();
    }

    public List<BlockState> states() {
        return states;
    }

    public BlockState state(int id) {
        if (id < 0 || id >= states.size()) {
            throw new IllegalArgumentException("Unknown block state id: " + id);
        }
        return states.get(id);
    }

    public int requireId(BlockState state) {
        return id(state).orElseThrow(() -> new IllegalArgumentException("Unknown or invalid block state: " + state));
    }

    public OptionalInt id(BlockState state) {
        Objects.requireNonNull(state, "state");
        Integer id = idsByState.get(state);
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    public BlockState defaultState(ResourceId blockId) {
        BlockState state = defaultStates.get(Objects.requireNonNull(blockId, "blockId"));
        if (state == null) {
            throw new IllegalArgumentException("Unknown block id: " + blockId);
        }
        return state;
    }

    public BlockState state(ResourceId blockId, Map<String, String> properties) {
        BlockState state = new BlockState(blockId, properties);
        requireId(state);
        return state;
    }

    public int airStateId() {
        return requireId(BlockState.AIR);
    }

    public CompactBlockStorage createStorage() {
        return new CompactBlockStorage(this::requireId, this::state, airStateId());
    }

    private static List<BlockState> statesFor(ResourceId blockId, BlockStateDefinition definition) {
        if (definition.isEmpty()) {
            return List.of(new BlockState(blockId));
        }

        List<String> propertyNames = definition.properties().keySet().stream()
                .sorted()
                .toList();
        List<BlockState> states = new ArrayList<>();
        addStates(states, blockId, definition, propertyNames, 0, new LinkedHashMap<>());
        return states;
    }

    private static void addStates(
            List<BlockState> states,
            ResourceId blockId,
            BlockStateDefinition definition,
            List<String> propertyNames,
            int propertyIndex,
            LinkedHashMap<String, String> properties
    ) {
        if (propertyIndex == propertyNames.size()) {
            states.add(new BlockState(blockId, Map.copyOf(properties)));
            return;
        }

        String property = propertyNames.get(propertyIndex);
        for (String value : definition.properties().get(property)) {
            properties.put(property, value);
            addStates(states, blockId, definition, propertyNames, propertyIndex + 1, properties);
        }
        properties.remove(property);
    }
}
