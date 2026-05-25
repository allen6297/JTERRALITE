package com.terralite.engine.player;

import com.terralite.engine.entity.ComponentType;

public final class PlayerComponents {
    public static final ComponentType<PlayerControlled> PLAYER_CONTROLLED =
        ComponentType.of("terralite:player_controlled", PlayerControlled.class);

    private PlayerComponents() {
    }
}
