package com.terralite.engine.physics

import com.terralite.engine.entity.Entity

@JvmRecord
data class CollisionPair(val first: Entity, val second: Entity) {
    init {
        require(first !== second && first.id != second.id) {
            "Collision pair requires two distinct entities"
        }
    }
}
