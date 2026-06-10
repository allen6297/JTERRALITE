package com.terralite.launcher.net;

/** Client → server: local player's current position, yaw and head pitch. Sent every frame. */
public record PlayerMoveMessage(double x, double y, double z, double yaw, double pitch) implements NetMessage {}
