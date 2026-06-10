package com.terralite.launcher.net;

/** Client → server: break the block at the given world position. */
public record RemoveBlockMessage(int x, int y, int z) implements NetMessage {}
