package com.terralite.runtime.render;

import com.terralite.engine.camera.Camera;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.Viewport;
import com.terralite.render.mesh.DebugMesh;

import java.util.List;
import java.util.Locale;

/**
 * Builds the chat / console overlay mesh.
 *
 * <p>When {@code inputActive} is {@code true} the input bar ("> " + buffer + cursor)
 * is shown at the bottom-left of the screen. The message log is displayed above it.
 * When inactive only the log is shown (useful for a brief fade-out period).
 *
 * <p>Pass this mesh to {@code RenderPipeline.setUiOverlays()} each frame;
 * omit it to hide the overlay entirely.
 */
public final class ChatOverlay {

    private static final RenderChunk CHUNK      = new RenderChunk(Integer.MIN_VALUE + 3, 0, 0);
    private static final int         MAX_VISIBLE = 8;
    private static final int         MAX_CHARS   = 40;

    private ChatOverlay() {}

    /**
     * @param camera      current camera
     * @param viewport    current viewport
     * @param messages    all chat messages, oldest first
     * @param inputBuffer current typed text (without prompt prefix)
     * @param inputActive whether the input bar should be shown
     */
    public static RenderChunkMesh build(Camera camera, Viewport viewport,
                                        List<String> messages, String inputBuffer,
                                        boolean inputActive) {
        ScreenCanvas c = new ScreenCanvas(camera, viewport);
        float H  = c.viewHeight();
        float W  = c.viewWidth();

        float charH = H * 0.038f;
        float cellW = charH * 0.62f;
        float lineH = charH * 1.35f;
        float padX  = charH * 0.55f;
        float padY  = charH * 0.4f;

        // Anchor to bottom-left corner
        float areaLeft  = -W * 0.5f + padX;
        float inputTop  = -H * 0.5f + padY + charH;  // baseline of input row

        // ---- Input bar --------------------------------------------------------
        // Text drawn at baselineY spans baselineY (bottom) → baselineY+charH (top).
        // Background quads must cover that same range (+ small pad).
        float rowPad = charH * 0.2f;
        float rowH   = charH + rowPad * 2;   // tight height around a single text row

        if (inputActive) {
            String prompt = "> " + inputBuffer + "_";
            float  barW   = MAX_CHARS * cellW + padX * 2;
            float  barTop = inputTop + charH + rowPad;   // above text top

            // dark background
            c.addQuad(areaLeft - padX * 0.6f, barTop, barW, rowH,
                      0f, 0.0f, 0.0f, 0.0f, 0.78f);
            // left accent stripe (same bounds, slightly in front)
            c.addQuad(areaLeft - padX * 0.6f, barTop, cellW * 0.3f, rowH,
                      -0.001f, 0.3f, 0.9f, 0.55f, 1.0f);
            c.addText(prompt, areaLeft, inputTop, charH, 0.85f, 1.0f, 0.75f, 1.0f);
        }

        // ---- Message log -------------------------------------------------------
        List<String> all = messages.stream()
                .map(m -> m.length() > MAX_CHARS
                        ? m.substring(0, MAX_CHARS - 3) + "..."
                        : m)
                .toList();

        // Show the most-recent MAX_VISIBLE messages, displayed oldest→newest top→bottom
        int start = Math.max(0, all.size() - MAX_VISIBLE);
        List<String> visible = all.subList(start, all.size());

        float msgBottomY = inputTop + lineH + charH * 0.25f; // just above the input bar
        float msgY = msgBottomY + visible.size() * lineH;    // top of first (oldest) message

        float logAlpha = inputActive ? 0.95f : 0.72f;

        for (String msg : visible) {
            String upper  = msg.toUpperCase(Locale.ROOT);
            float  bgW    = upper.length() * cellW + padX * 2;
            float  bgTop  = msgY + charH + rowPad;   // above text top
            c.addQuad(areaLeft - padX * 0.6f, bgTop, bgW, rowH,
                      0f, 0.0f, 0.0f, 0.0f, 0.52f);
            c.addText(upper, areaLeft, msgY, charH, 1.0f, 1.0f, 1.0f, logAlpha);
            msgY -= lineH;
        }

        return new RenderChunkMesh(CHUNK, new DebugMesh(c.vertices()));
    }
}
