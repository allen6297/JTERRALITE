package com.terralite.runtime.interaction;

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

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
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
    private java.util.function.BiConsumer<BlockPos, BlockState> onBlockPlaced = (p, s) -> {};
    private java.util.function.Consumer<BlockPos> onBlockBroken = p -> {};

    private int breakingTicks = 0;
    private BlockPos currentBreakTarget = null;
    private boolean wasPlacing = false;
    private BlockRaycaster.HitResult currentTarget = null;
    private Ray inspectionRay = null;

    public record Ray(double originX, double originY, double originZ, double dirX, double dirY, double dirZ) {
    }

    public @Nullable BlockRaycaster.HitResult currentTarget() {
        return currentTarget;
    }

    public BlockInteractionSystem onBlockPlaced(java.util.function.BiConsumer<BlockPos, BlockState> callback) {
        this.onBlockPlaced = Objects.requireNonNull(callback, "callback");
        return this;
    }

    public BlockInteractionSystem onBlockBroken(java.util.function.Consumer<BlockPos> callback) {
        this.onBlockBroken = Objects.requireNonNull(callback, "callback");
        return this;
    }

    public void setInspectionRay(Ray inspectionRay) {
        this.inspectionRay = inspectionRay;
    }

    public void refreshTarget(World world) {
        currentTarget = castTarget(Objects.requireNonNull(world, "world"));
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
        boolean inspecting = inspectionRay != null;
        boolean breaking = !inspecting && input.isPressed(InputActions.BREAK_BLOCK);
        boolean placing  = !inspecting && input.isPressed(InputActions.PLACE_BLOCK);

        var hit = castTarget(world);
        currentTarget = hit;

        if (breaking) {
            if (hit != null) {
                if (!hit.blockPos().equals(currentBreakTarget)) {
                    currentBreakTarget = hit.blockPos();
                    breakingTicks = 0;
                }
                breakingTicks++;
                if (breakingTicks >= BREAK_TIME_TICKS) {
                    world.removeBlock(hit.blockPos());
                    onBlockBroken.accept(hit.blockPos());
                    onBlockChanged.accept(chunkPosFor(hit.blockPos()));
                    breakingTicks = 0;
                    currentBreakTarget = null;
                }
            } else {
                currentBreakTarget = null;
                breakingTicks = 0;
            }
        } else {
            currentBreakTarget = null;
            breakingTicks = 0;
        }

        if (placing && !wasPlacing && hit != null) {
            BlockPos adjacent = hit.adjacentPos();
            if (!world.blocks().contains(adjacent)) {
                BlockState placed = new BlockState(placeBlockId);
                world.setBlock(adjacent, placed);
                onBlockPlaced.accept(adjacent, placed);
                onBlockChanged.accept(chunkPosFor(adjacent));
            }
        }

        wasPlacing = placing;
    }

    private @Nullable BlockRaycaster.HitResult castTarget(World world) {
        double cx;
        double cy;
        double cz;
        double dirX;
        double dirY;
        double dirZ;
        if (inspectionRay != null) {
            cx = inspectionRay.originX();
            cy = inspectionRay.originY();
            cz = inspectionRay.originZ();
            dirX = inspectionRay.dirX();
            dirY = inspectionRay.dirY();
            dirZ = inspectionRay.dirZ();
        } else {
            double yawRad   = Math.toRadians(camera.yaw());
            double pitchRad = Math.toRadians(camera.pitch());
            dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
            dirY =  Math.sin(pitchRad);
            dirZ = -Math.cos(yawRad) * Math.cos(pitchRad);

            // Always cast from the player's eye position so third-person works correctly
            Transform playerTransformNullable = world.entities().require(playerId).get(PhysicsComponents.TRANSFORM);
            Transform playerTransform = playerTransformNullable != null ? playerTransformNullable : Transform.ORIGIN;
            cx = playerTransform.x();
            cy = playerTransform.y() + EYE_HEIGHT;
            cz = playerTransform.z();
        }

        return BlockRaycaster.cast(world, cx, cy, cz, dirX, dirY, dirZ, REACH);
    }

    private static ChunkPos chunkPosFor(BlockPos pos) {
        return ChunkPos.of(
                Math.floorDiv(pos.x(), RuntimeWorldFactory.CHUNK_SIZE),
                Math.floorDiv(pos.y(), RuntimeWorldFactory.CHUNK_SIZE),
                Math.floorDiv(pos.z(), RuntimeWorldFactory.CHUNK_SIZE)
        );
    }
}
