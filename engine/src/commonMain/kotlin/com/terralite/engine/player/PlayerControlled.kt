package com.terralite.engine.player

@JvmRecord
data class PlayerControlled(val movementSpeed: Double) {
    init {
        require(movementSpeed >= 0.0) { "Movement speed cannot be negative" }
    }
}
