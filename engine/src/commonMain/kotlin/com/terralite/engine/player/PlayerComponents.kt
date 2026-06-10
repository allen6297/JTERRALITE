package com.terralite.engine.player

import com.terralite.engine.entity.ComponentType

object PlayerComponents {
    @JvmField val PLAYER_CONTROLLED: ComponentType<PlayerControlled> = ComponentType.of("terralite:player_controlled")
}
