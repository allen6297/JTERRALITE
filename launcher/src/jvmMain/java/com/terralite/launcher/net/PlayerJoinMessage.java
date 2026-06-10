package com.terralite.launcher.net;

/** Server → client: a new player connected. */
public record PlayerJoinMessage(String playerId, double x, double y, double z, double yaw, double pitch) implements NetMessage {
    /** Convenience constructor for join events where look direction is not yet known. */
    public PlayerJoinMessage(String playerId, double x, double y, double z) {
        this(playerId, x, y, z, 0.0, 0.0);
    }
}
