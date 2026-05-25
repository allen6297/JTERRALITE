package com.terralite.engine.chunk;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryChunkStorage implements ChunkStorage {
    private final Map<ChunkPos, Chunk> chunks = new LinkedHashMap<>();

    @Override
    public Optional<Chunk> get(ChunkPos pos) {
        return Optional.ofNullable(chunks.get(Objects.requireNonNull(pos, "pos")));
    }

    @Override
    public boolean contains(ChunkPos pos) {
        return chunks.containsKey(Objects.requireNonNull(pos, "pos"));
    }

    @Override
    public Chunk put(Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        chunks.put(chunk.pos(), chunk);
        return chunk;
    }

    @Override
    public Chunk remove(ChunkPos pos) {
        Chunk removed = chunks.remove(Objects.requireNonNull(pos, "pos"));
        if (removed == null) {
            throw new IllegalArgumentException("Missing chunk: " + pos);
        }
        return removed;
    }

    @Override
    public Chunk require(ChunkPos pos) {
        return get(pos).orElseThrow(() -> new IllegalArgumentException("Missing chunk: " + pos));
    }

    @Override
    public Collection<ChunkPos> positions() {
        return List.copyOf(chunks.keySet());
    }

    @Override
    public Collection<Chunk> chunks() {
        return List.copyOf(chunks.values());
    }

    @Override
    public int size() {
        return chunks.size();
    }
}
