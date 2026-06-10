package com.terralite.engine.save

import com.terralite.engine.chunk.Chunk
import com.terralite.engine.entity.Entity
import com.terralite.engine.entity.EntityId
import com.terralite.engine.terrain.BlockPos
import com.terralite.engine.terrain.BlockStorage
import com.terralite.engine.world.World

class WorldSnapshotter {
    fun snapshot(world: World): WorldSnapshot {
        val entities = world.entities().entities()
            .map { EntitySnapshot(it.id) }

        val blocks = world.blocks().positions()
            .map { pos -> snapshotBlock(world, pos) }

        return WorldSnapshot(world.chunkPositions().toList(), entities, blocks)
    }

    fun restore(snapshot: WorldSnapshot): World = restore(snapshot, null)

    fun restore(snapshot: WorldSnapshot, blockStorage: BlockStorage?): World {
        val world = if (blockStorage == null) World() else World(blockStorage)
        for (chunkPos in snapshot.chunks) {
            world.putChunk(Chunk(chunkPos))
        }
        for (block in snapshot.blocks) {
            world.setBlock(block.pos, block.state)
        }
        for (entity in snapshot.entities) {
            world.entities().add(Entity(EntityId.of(entity.id.value)))
        }
        return world
    }

    private companion object {
        fun snapshotBlock(world: World, pos: BlockPos): BlockSnapshot {
            val stateId = world.blocks().stateId(pos)
            return BlockSnapshot(pos, world.getBlock(pos), stateId)
        }
    }
}
