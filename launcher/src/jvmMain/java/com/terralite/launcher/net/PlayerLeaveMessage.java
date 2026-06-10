package com.terralite.launcher.net;

/** Server → client: a player disconnected. */
public record PlayerLeaveMessage(String playerId) implements NetMessage {}
