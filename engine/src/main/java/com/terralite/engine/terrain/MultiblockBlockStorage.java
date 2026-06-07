package com.terralite.engine.terrain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Function;

public final class MultiblockBlockStorage implements BlockStorage {
    private static final List<BlockPos> SINGLE_BLOCK = List.of(BlockPos.of(0, 0, 0));

    private final BlockStorage origins;
    private final Function<BlockState, List<BlockPos>> occupancy;
    private final Map<BlockPos, BlockPos> occupiedOrigins = new LinkedHashMap<>();

    public MultiblockBlockStorage(BlockStorage origins, Function<BlockState, List<BlockPos>> occupancy) {
        this.origins = Objects.requireNonNull(origins, "origins");
        this.occupancy = Objects.requireNonNull(occupancy, "occupancy");
    }

    @Override
    public BlockState get(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        if (origins.contains(pos)) {
            return origins.get(pos);
        }
        BlockPos origin = occupiedOrigins.get(pos);
        return origin == null ? BlockState.AIR : origins.get(origin);
    }

    @Override
    public void set(BlockPos pos, BlockState state) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (state.isAir()) {
            remove(pos);
            return;
        }

        BlockPos existingOrigin = originOf(pos);
        if (existingOrigin != null) {
            if (!existingOrigin.equals(pos)) {
                throw new IllegalStateException(
                        "Cannot place " + state.id() + " at " + pos + "; position belongs to " + existingOrigin
                );
            }
            remove(existingOrigin);
        }

        List<BlockPos> offsets = occupancyFor(state);
        List<BlockPos> occupiedPositions = offsets.stream()
                .map(offset -> add(pos, offset))
                .toList();
        for (BlockPos occupied : occupiedPositions) {
            BlockPos blockingOrigin = originOf(occupied);
            if (blockingOrigin != null) {
                throw new IllegalStateException(
                        "Cannot place " + state.id() + " at " + pos + "; occupied position "
                                + occupied + " belongs to " + blockingOrigin
                );
            }
        }

        origins.set(pos, state);
        for (BlockPos occupied : occupiedPositions) {
            if (!occupied.equals(pos)) {
                occupiedOrigins.put(occupied, pos);
            }
        }
    }

    @Override
    public BlockState remove(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        BlockPos origin = originOf(pos);
        if (origin == null) {
            return BlockState.AIR;
        }

        BlockState removed = origins.remove(origin);
        for (BlockPos occupied : occupiedPositions(origin, removed)) {
            occupiedOrigins.remove(occupied);
        }
        return removed;
    }

    @Override
    public OptionalInt stateId(BlockPos pos) {
        BlockPos origin = originOf(Objects.requireNonNull(pos, "pos"));
        return origin == null ? OptionalInt.empty() : origins.stateId(origin);
    }

    @Override
    public boolean contains(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        return origins.contains(pos) || occupiedOrigins.containsKey(pos);
    }

    @Override
    public Collection<BlockPos> positions() {
        return origins.positions();
    }

    @Override
    public int size() {
        return origins.size();
    }

    public boolean isOrigin(BlockPos pos) {
        return origins.contains(Objects.requireNonNull(pos, "pos"));
    }

    public BlockPos originOf(BlockPos pos) {
        Objects.requireNonNull(pos, "pos");
        if (origins.contains(pos)) {
            return pos;
        }
        return occupiedOrigins.get(pos);
    }

    public Collection<BlockPos> occupiedPositions(BlockPos origin) {
        Objects.requireNonNull(origin, "origin");
        BlockState state = origins.get(origin);
        if (state.isAir()) {
            return List.of();
        }
        return occupiedPositions(origin, state);
    }

    private List<BlockPos> occupiedPositions(BlockPos origin, BlockState state) {
        return occupancyFor(state).stream()
                .map(offset -> add(origin, offset))
                .toList();
    }

    private List<BlockPos> occupancyFor(BlockState state) {
        List<BlockPos> offsets = occupancy.apply(state);
        if (offsets == null || offsets.isEmpty()) {
            return SINGLE_BLOCK;
        }
        return List.copyOf(offsets);
    }

    private static BlockPos add(BlockPos pos, BlockPos offset) {
        return BlockPos.of(pos.x() + offset.x(), pos.y() + offset.y(), pos.z() + offset.z());
    }
}
