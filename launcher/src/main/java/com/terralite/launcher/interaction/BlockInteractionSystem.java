package com.terralite.launcher.interaction;

import com.terralite.core.registry.ResourceId;
import com.terralite.engine.camera.Camera;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.input.InputActions;
import com.terralite.engine.input.InputState;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockRaycaster;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.world.World;
import com.terralite.runtime.world.RuntimeWorldFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Handles left-click block breaking and right-click block placing.
 *
 * <p>Breaking requires holding the mouse button for {@code BREAK_TIME_TICKS} ticks.
 * Placing is instant on the leading edge of right-click.
 */
public final class BlockInteractionSystem implements WorldSimulationSystem {
    private static final double REACH = 6.0;
    private static final int BREAK_TIME_TICKS = 4;
    // Must match PlayerCameraSystem.EYE_HEIGHT
    private static final double EYE_HEIGHT = 0.6;

    private final InputState input;
    private final Camera camera;
    private final EntityId playerId;
    private final ResourceId placeBlockId;
    private final Consumer<ChunkPos> onBlockChanged;

    private int breakingTicks = 0;
    private BlockPos currentBreakTarget = null;
    private boolean wasPlacing = false;
    private BlockRaycaster.HitResult currentTarget = null;

    public Optional<BlockRaycaster.HitResult> currentTarget() {
        return Optional.ofNullable(currentTarget);
    }

    public BlockInteractionSystem(InputState input, Camera camera, EntityId playerId,
                                   ResourceId placeBlockId, Consumer<ChunkPos> onBlockChanged) {
        this.input = Objects.requireNonNull(input, "input");
        this.camera = Objects.requireNonNull(camera, "camera");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.placeBlockId = Objects.requireNonNull(placeBlockId, "placeBlockId");
        this.onBlockChanged = Objects.requireNonNull(onBlockChanged, "onBlockChanged");
    }

    @Override
    public void tick(World world, SimulationTick tick) {
        boolean breaking = input.isPressed(InputActions.BREAK_BLOCK);
        boolean placing  = input.isPressed(InputActions.PLACE_BLOCK);

        double yawRad   = Math.toRadians(camera.yaw());
        double pitchRad = Math.toRadians(camera.pitch());
        double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY =  Math.sin(pitchRad);
        double dirZ = -Math.cos(yawRad) * Math.cos(pitchRad);

        // Always cast from the player's eye position so third-person works correctly
        Transform playerTransform = world.entities().require(playerId)
                .get(PhysicsComponents.TRANSFORM).orElse(Transform.ORIGIN);
        double cx = playerTransform.x();
        double cy = playerTransform.y() + EYE_HEIGHT;
        double cz = playerTransform.z();

        var hit = BlockRaycaster.cast(world, cx, cy, cz, dirX, dirY, dirZ, REACH);
        currentTarget = hit.orElse(null);

        if (breaking) {
            hit.ifPresentOrElse(h -> {
                if (!h.blockPos().equals(currentBreakTarget)) {
                    currentBreakTarget = h.blockPos();
                    breakingTicks = 0;
                }
                breakingTicks++;
                if (breakingTicks >= BREAK_TIME_TICKS) {
                    world.removeBlock(h.blockPos());
                    onBlockChanged.accept(chunkPosFor(h.blockPos()));
                    breakingTicks = 0;
                    currentBreakTarget = null;
                }
            }, () -> {
                currentBreakTarget = null;
                breakingTicks = 0;
            });
        } else {
            currentBreakTarget = null;
            breakingTicks = 0;
        }

        if (placing && !wasPlacing) {
            hit.ifPresent(h -> {
                BlockPos adjacent = h.adjacentPos();
                if (!world.blocks().contains(adjacent)) {
                    world.setBlock(adjacent, new BlockState(placeBlockId));
                    onBlockChanged.accept(chunkPosFor(adjacent));
                }
            });
        }

        wasPlacing = placing;
    }

    private static ChunkPos chunkPosFor(BlockPos pos) {
        return ChunkPos.of(
                Math.floorDiv(pos.x(), RuntimeWorldFactory.CHUNK_SIZE),
                Math.floorDiv(pos.y(), RuntimeWorldFactory.CHUNK_SIZE),
                Math.floorDiv(pos.z(), RuntimeWorldFactory.CHUNK_SIZE)
        );
    }
}
