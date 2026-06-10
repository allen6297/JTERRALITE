package com.terralite.launcher.net;

/** Client → server: place a block at the given world position. */
public record SetBlockMessage(int x, int y, int z, String id) implements NetMessage {}
