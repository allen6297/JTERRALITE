package com.terralite.launcher.net;

/** Server → client: position and look-direction update for a remote player. */
public record PlayerPositionMessage(String playerId, double x, double y, double z, double yaw, double pitch) implements NetMessage {}
