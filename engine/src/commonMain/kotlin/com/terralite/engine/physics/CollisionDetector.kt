package com.terralite.engine.physics

import com.terralite.engine.entity.Entity
import com.terralite.engine.terrain.BlockPos
import com.terralite.engine.world.World

class CollisionDetector {
    fun detect(world: World): List<CollisionPair> {
        val collidable = world.entities().entities()
            .filter { it.has(PhysicsComponents.TRANSFORM) && it.has(PhysicsComponents.COLLIDER) }
        val collisions = mutableListOf<CollisionPair>()
        for (i in collidable.indices) {
            val first = collidable.elementAt(i)
            val firstBounds = bounds(first)
            for (j in i + 1 until collidable.size) {
                val second = collidable.elementAt(j)
                if (firstBounds.intersects(bounds(second))) {
                    collisions += CollisionPair(first, second)
                }
            }
        }
        return collisions.toList()
    }

    fun detectBlockCollisions(world: World): List<BlockCollision> {
        val collidable = world.entities().entities()
            .filter { it.has(PhysicsComponents.TRANSFORM) && it.has(PhysicsComponents.COLLIDER) }
        val blockPositions = world.collisionBlockPositions().toList()
        val collisions = mutableListOf<BlockCollision>()
        for (entity in collidable) {
            val entityBounds = bounds(entity)
            for (blockPos in blockPositions) {
                val blockBounds = blockBounds(blockPos)
                if (entityBounds.intersects(blockBounds)) {
                    collisions += BlockCollision(entity, blockPos, blockBounds)
                }
            }
        }
        return collisions.toList()
    }

    private companion object {
        fun bounds(entity: Entity): Aabb {
            val transform = entity.require(PhysicsComponents.TRANSFORM)
            val collider = entity.require(PhysicsComponents.COLLIDER)
            return collider.boundsAt(transform)
        }

        fun blockBounds(pos: BlockPos): Aabb =
            Aabb(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                 pos.x + 1.0, pos.y + 1.0, pos.z + 1.0)
    }
}
