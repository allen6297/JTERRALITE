package com.terralite.engine.physics;

import com.terralite.engine.entity.Entity;
import com.terralite.engine.simulation.SimulationTick;
import com.terralite.engine.simulation.WorldSimulationSystem;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.world.World;

/**
 * Resolves entity-vs-block AABB penetrations after {@link MovementSystem} runs.
 *
 * <p>For each overlapping block, the minimum-penetration axis is pushed out.
 * An upward Y push sets {@link PhysicsComponents#GROUNDED} and zeros downward
 * Y velocity so jump logic works correctly.
 */
public final class BlockCollisionResolutionSystem implements WorldSimulationSystem {

    @Override
    public void tick(World world, SimulationTick tick) {
        for (Entity entity : world.entities().entities()) {
            if (!entity.has(PhysicsComponents.TRANSFORM) || !entity.has(PhysicsComponents.COLLIDER)) {
                continue;
            }
            resolveEntity(world, entity);
        }
    }

    private static void resolveEntity(World world, Entity entity) {
        Collider collider = entity.require(PhysicsComponents.COLLIDER);
        Transform t = entity.require(PhysicsComponents.TRANSFORM);
        double tx = t.x(), ty = t.y(), tz = t.z();
        boolean grounded = false;

        Aabb initial = collider.boundsAt(t);
        int minBx = (int) Math.floor(initial.minX()) - 1;
        int minBy = (int) Math.floor(initial.minY()) - 1;
        int minBz = (int) Math.floor(initial.minZ()) - 1;
        int maxBx = (int) Math.ceil(initial.maxX());
        int maxBy = (int) Math.ceil(initial.maxY());
        int maxBz = (int) Math.ceil(initial.maxZ());

        for (int bx = minBx; bx <= maxBx; bx++) {
            for (int by = minBy; by <= maxBy; by++) {
                for (int bz = minBz; bz <= maxBz; bz++) {
                    if (!world.blocks().contains(BlockPos.of(bx, by, bz))) continue;

                    Aabb block = new Aabb(bx, by, bz, bx + 1.0, by + 1.0, bz + 1.0);
                    Aabb bounds = collider.boundsAt(new Transform(tx, ty, tz));
                    if (!bounds.intersects(block)) continue;

                    double ox = Math.min(bounds.maxX() - block.minX(), block.maxX() - bounds.minX());
                    double oy = Math.min(bounds.maxY() - block.minY(), block.maxY() - bounds.minY());
                    double oz = Math.min(bounds.maxZ() - block.minZ(), block.maxZ() - bounds.minZ());

                    if (oy <= ox && oy <= oz) {
                        if (ty > by + 0.5) {
                            // Entity came from above — stand on top
                            ty += oy;
                            grounded = true;
                            cancelNegativeY(entity);
                        } else {
                            // Hit ceiling — bump down
                            ty -= oy;
                            cancelPositiveY(entity);
                        }
                    } else if (ox <= oz) {
                        tx += (tx > bx + 0.5) ? ox : -ox;
                    } else {
                        tz += (tz > bz + 0.5) ? oz : -oz;
                    }
                }
            }
        }

        entity.set(PhysicsComponents.TRANSFORM, new Transform(tx, ty, tz));
        entity.set(PhysicsComponents.GROUNDED, grounded);
    }

    private static void cancelNegativeY(Entity entity) {
        entity.get(PhysicsComponents.VELOCITY)
                .filter(v -> v.y() < 0)
                .ifPresent(v -> entity.set(PhysicsComponents.VELOCITY, new Velocity(v.x(), 0.0, v.z())));
    }

    private static void cancelPositiveY(Entity entity) {
        entity.get(PhysicsComponents.VELOCITY)
                .filter(v -> v.y() > 0)
                .ifPresent(v -> entity.set(PhysicsComponents.VELOCITY, new Velocity(v.x(), 0.0, v.z())));
    }
}
