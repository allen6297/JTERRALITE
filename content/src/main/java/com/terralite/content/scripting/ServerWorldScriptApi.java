package com.terralite.content.scripting;

public interface ServerWorldScriptApi {
    int entityCount();

    int chunkCount();

    boolean hasChunk(int x, int y, int z);

    boolean loadChunk(int x, int y, int z);

    boolean unloadChunk(int x, int y, int z);
}
