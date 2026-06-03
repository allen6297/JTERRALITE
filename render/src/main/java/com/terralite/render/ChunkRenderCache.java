package com.terralite.render;

import com.terralite.render.vulkan.VulkanContext;
import com.terralite.render.vulkan.VulkanMeshBuffer;
import com.terralite.render.mesh.DebugMesh;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tracks GPU-resident chunk meshes, their signatures, and lifecycle state.
 * Supports eviction after N unseen frames and deferred destruction.
 */
public final class ChunkRenderCache {
    private final Map<RenderChunk, ChunkRenderEntry> entries = new HashMap<>();
    private final int evictionThreshold;

    public ChunkRenderCache(int evictionThreshold) {
        if (evictionThreshold < 1) {
            throw new IllegalArgumentException("evictionThreshold must be >= 1");
        }
        this.evictionThreshold = evictionThreshold;
    }

    /**
     * Marks all entries as not visible and not pending upload for the upcoming frame.
     * Call once at the start of each frame's prepare phase.
     */
    public void beginFrame(long frameIndex) {
        for (ChunkRenderEntry entry : entries.values()) {
            entry.beginFrame();
        }
    }

    /**
     * Returns the entry for a chunk, or null if not cached.
     */
    public ChunkRenderEntry get(RenderChunk chunk) {
        return entries.get(chunk);
    }

    /**
     * Creates a new GPU buffer for a chunk mesh and stores it in the cache.
     */
    public ChunkRenderEntry create(VulkanContext ctx, RenderChunk chunk, MeshSignature signature, DebugMesh mesh) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(signature, "signature");
        Objects.requireNonNull(mesh, "mesh");

        VulkanMeshBuffer buffer = VulkanMeshBuffer.create(ctx, mesh);
        ChunkRenderEntry entry = new ChunkRenderEntry(signature, buffer, mesh.vertices().size());
        entries.put(chunk, entry);
        return entry;
    }

    /**
     * Removes an entry and destroys its GPU buffer immediately.
     * For safe deferred destruction, use {@link #removeDeferred}.
     */
    public void remove(VulkanContext ctx, RenderChunk chunk) {
        ChunkRenderEntry entry = entries.remove(chunk);
        if (entry != null) {
            entry.destroy(ctx);
        }
    }

    /**
     * Marks an entry for deferred destruction. The caller must later call
     * {@link #flushDeferredDestruction} after the relevant frame fence completes.
     */
    public void removeDeferred(RenderChunk chunk, long retireFrame) {
        ChunkRenderEntry entry = entries.get(chunk);
        if (entry != null) {
            entry.markRetired(retireFrame);
        }
    }

    /**
     * Destroys all entries whose retirement frame is <= the completed frame.
     */
    public void flushDeferredDestruction(VulkanContext ctx, long completedFrame) {
        entries.entrySet().removeIf(e -> {
            ChunkRenderEntry entry = e.getValue();
            if (entry.isRetired() && entry.retireFrame() <= completedFrame) {
                entry.destroy(ctx);
                return true;
            }
            return false;
        });
    }

    /**
     * Evicts entries that have not been seen for {@code evictionThreshold} frames.
     */
    public void evictStale(VulkanContext ctx, long currentFrame) {
        entries.entrySet().removeIf(e -> {
            ChunkRenderEntry entry = e.getValue();
            if (!entry.visibleThisFrame() && (currentFrame - entry.lastSeenFrame()) >= evictionThreshold) {
                entry.destroy(ctx);
                return true;
            }
            return false;
        });
    }

    /**
     * Destroys all entries immediately. Use during shutdown.
     */
    public void destroyAll(VulkanContext ctx) {
        for (ChunkRenderEntry entry : entries.values()) {
            entry.destroy(ctx);
        }
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}