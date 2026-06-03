package com.terralite.render;

import com.terralite.render.vulkan.VulkanContext;
import com.terralite.render.vulkan.VulkanMeshBuffer;

import java.util.Objects;

/**
 * GPU-resident state for a single chunk mesh.
 * Tracks signature, residency, visibility, and retirement for lifecycle management.
 */
public final class ChunkRenderEntry {
    private final MeshSignature signature;
    private final VulkanMeshBuffer buffer;
    private final int vertexCount;
    private long lastSeenFrame;
    private boolean visibleThisFrame;
    private boolean pendingUpload;
    private boolean retired;
    private long retireFrame;

    ChunkRenderEntry(MeshSignature signature, VulkanMeshBuffer buffer, int vertexCount) {
        this.signature = Objects.requireNonNull(signature, "signature");
        this.buffer = Objects.requireNonNull(buffer, "buffer");
        this.vertexCount = vertexCount;
        this.lastSeenFrame = -1;
        this.visibleThisFrame = false;
        this.pendingUpload = false;
        this.retired = false;
        this.retireFrame = -1;
    }

    public MeshSignature signature() {
        return signature;
    }

    public VulkanMeshBuffer buffer() {
        return buffer;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public long lastSeenFrame() {
        return lastSeenFrame;
    }

    public boolean visibleThisFrame() {
        return visibleThisFrame;
    }

    public boolean pendingUpload() {
        return pendingUpload;
    }

    public boolean isRetired() {
        return retired;
    }

    public long retireFrame() {
        return retireFrame;
    }

    public void markSeen(long frameIndex) {
        this.lastSeenFrame = frameIndex;
    }

    public void markVisible() {
        this.visibleThisFrame = true;
    }

    void markPendingUpload() {
        this.pendingUpload = true;
    }

    void clearPendingUpload() {
        this.pendingUpload = false;
    }

    void markRetired(long retireFrame) {
        this.retired = true;
        this.retireFrame = retireFrame;
    }

    void beginFrame() {
        this.visibleThisFrame = false;
        this.pendingUpload = false;
    }

    void destroy(VulkanContext ctx) {
        buffer.destroy(ctx);
    }
}