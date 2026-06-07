package com.terralite.render;

import com.terralite.render.math.FrustumCuller;
import com.terralite.render.vulkan.VulkanTextureAtlas;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of everything the command recorder needs for one frame.
 * Created during the prepare phase so that command recording is pure GPU API work.
 */
public record PreparedRenderFrame(
        Viewport viewport,
        float[] mvp,
        FrustumCuller frustum,
        List<ChunkDraw> visibleChunkDraws,
        VulkanTextureAtlas textureAtlas,
        ClearColor clearColor
) {
    public PreparedRenderFrame {
        Objects.requireNonNull(viewport, "viewport");
        Objects.requireNonNull(mvp, "mvp");
        Objects.requireNonNull(frustum, "frustum");
        Objects.requireNonNull(visibleChunkDraws, "visibleChunkDraws");
        Objects.requireNonNull(clearColor, "clearColor");
    }

    public int drawCount() {
        return visibleChunkDraws.size();
    }
}