package com.terralite.render;

import com.terralite.render.vulkan.VulkanContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Queues GPU resources for destruction only after their last in-flight frame completes.
 * Prevents use-after-free when resources are replaced mid-frame.
 */
public final class DeferredReleaseQueue {
    private final Deque<ReleaseEntry> queue = new ArrayDeque<>();

    /**
     * Queues a chunk entry for deferred destruction. The entry will be destroyed
     * only after the fence for {@code frameIndex} has been waited on.
     */
    public void enqueue(ChunkRenderEntry entry, long frameIndex) {
        Objects.requireNonNull(entry, "entry");
        queue.addLast(new ReleaseEntry(entry, frameIndex));
    }

    /**
     * Destroys all entries whose retirement frame is <= the completed frame.
     * Call after waiting on the frame fence.
     */
    public void flush(VulkanContext ctx, long completedFrame) {
        while (!queue.isEmpty() && queue.peekFirst().retireFrame <= completedFrame) {
            ReleaseEntry entry = queue.pollFirst();
            entry.entry.destroy(ctx);
        }
    }

    /**
     * Destroys everything immediately. Use during shutdown.
     */
    public void destroyAll(VulkanContext ctx) {
        while (!queue.isEmpty()) {
            queue.pollFirst().entry.destroy(ctx);
        }
    }

    public int size() {
        return queue.size();
    }

    private record ReleaseEntry(ChunkRenderEntry entry, long retireFrame) {
    }
}