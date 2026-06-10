package com.terralite.engine.physics

import com.terralite.engine.entity.Entity
import com.terralite.engine.simulation.SimulationTick
import com.terralite.engine.simulation.WorldSimulationSystem
import com.terralite.engine.terrain.BlockPos
import com.terralite.engine.world.World

/**
 * Resolves entity-vs-block AABB penetrations after [MovementSystem] runs.
 *
 * For each overlapping block, the minimum-penetration axis is pushed out.
 * An upward Y push sets [PhysicsComponents.GROUNDED] and zeros downward
 * Y velocity so jump logic works correctly.
 */
class BlockCollisionResolutionSystem : WorldSimulationSystem {
    override fun tick(world: World, tick: SimulationTick) {
        for (entity in world.entities().entities()) {
            if (!entity.has(PhysicsComponents.TRANSFORM) || !entity.has(PhysicsComponents.COLLIDER)) continue
            resolveEntity(world, entity)
        }
    }

    private fun resolveEntity(world: World, entity: Entity) {
        val collider = entity.require(PhysicsComponents.COLLIDER)
        val t = entity.require(PhysicsComponents.TRANSFORM)
        var tx = t.x; var ty = t.y; var tz = t.z
        var grounded = false

        val initial = collider.boundsAt(t)
        val minBx = Math.floor(initial.minX).toInt() - 1
        val minBy = Math.floor(initial.minY).toInt() - 1
        val minBz = Math.floor(initial.minZ).toInt() - 1
        val maxBx = Math.ceil(initial.maxX).toInt()
        val maxBy = Math.ceil(initial.maxY).toInt()
        val maxBz = Math.ceil(initial.maxZ).toInt()

        for (bx in minBx..maxBx) {
            for (by in minBy..maxBy) {
                for (bz in minBz..maxBz) {
                    if (!world.blocks().contains(BlockPos.of(bx, by, bz))) continue

                    val block = Aabb(bx.toDouble(), by.toDouble(), bz.toDouble(), bx + 1.0, by + 1.0, bz + 1.0)
                    val bounds = collider.boundsAt(Transform(tx, ty, tz))
                    if (!bounds.intersects(block)) continue

                    val ox = minOf(bounds.maxX - block.minX, block.maxX - bounds.minX)
                    val oy = minOf(bounds.maxY - block.minY, block.maxY - bounds.minY)
                    val oz = minOf(bounds.maxZ - block.minZ, block.maxZ - bounds.minZ)

                    if (oy <= ox && oy <= oz) {
                        if (ty > by + 0.5) {
                            ty += oy; grounded = true; cancelNegativeY(entity)
                        } else {
                            ty -= oy; cancelPositiveY(entity)
                        }
                    } else if (ox <= oz) {
                        tx += if (tx > bx + 0.5) ox else -ox
                    } else {
                        tz += if (tz > bz + 0.5) oz else -oz
                    }
                }
            }
        }

        entity.set(PhysicsComponents.TRANSFORM, Transform(tx, ty, tz))
        entity.set(PhysicsComponents.GROUNDED, grounded)
    }

    private fun cancelNegativeY(entity: Entity) {
        val v = entity.get(PhysicsComponents.VELOCITY) ?: return
        if (v.y < 0) entity.set(PhysicsComponents.VELOCITY, Velocity(v.x, 0.0, v.z))
    }

    private fun cancelPositiveY(entity: Entity) {
        val v = entity.get(PhysicsComponents.VELOCITY) ?: return
        if (v.y > 0) entity.set(PhysicsComponents.VELOCITY, Velocity(v.x, 0.0, v.z))
    }
}
