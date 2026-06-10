package com.terralite.runtime.render;

import com.terralite.engine.camera.Camera;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.Viewport;
import com.terralite.render.mesh.DebugMesh;

/**
 * Builds the pause-menu overlay mesh.
 *
 * <p>The caller is responsible for deciding when to show/hide this overlay;
 * simply omit it from {@code RenderPipeline.setUiOverlays()} to hide it.
 */
public final class PauseMenuOverlay {

    /** Menu items in display order. */
    public enum Item { RESUME, QUIT }

    private static final RenderChunk CHUNK = new RenderChunk(Integer.MIN_VALUE + 2, 0, 0);
    private static final String[] LABELS   = {"RESUME", "QUIT"};

    private PauseMenuOverlay() {}

    /**
     * Builds the pause-menu as a {@link RenderChunkMesh} suitable for
     * {@code RenderPipeline.setUiOverlays()}.
     *
     * @param camera   current camera (for screen-plane orientation)
     * @param viewport current viewport (for aspect ratio)
     * @param selected the currently highlighted menu item
     */
    public static RenderChunkMesh build(Camera camera, Viewport viewport, Item selected) {
        ScreenCanvas c = new ScreenCanvas(camera, viewport);
        float H = c.viewHeight();

        float charH    = H * 0.052f;
        float lineH    = charH * 1.9f;
        float padX     = charH * 1.4f;
        float padY     = charH * 1.1f;
        float titleH   = charH * 1.5f;
        float titleGap = charH * 0.7f;
        float cellW    = charH * 0.62f;

        // Panel sized to fit the title + items
        float longestLabel = 6; // "RESUME" = 6 chars
        float panelW = padX * 2 + (longestLabel + 4) * cellW; // room for "> RESUME"
        float panelH = padY * 2 + titleH + titleGap + LABELS.length * lineH;
        float left   = -panelW * 0.5f;
        float top    =  panelH * 0.5f;

        // Split the panel into two non-overlapping quads at the same depth to avoid
        // z-fighting AND the depth-offset displacement that caused the bar to float.
        float accentH = charH * 0.35f;
        // Accent band (top)
        c.addQuad(left, top, panelW, accentH, 0f, 0.35f, 0.55f, 0.95f, 0.95f);
        // Dark body (everything below the accent band)
        c.addQuad(left, top - accentH, panelW, panelH - accentH, 0f, 0.04f, 0.04f, 0.09f, 0.90f);

        // "PAUSED" title
        String title    = "PAUSED";
        float  titleW   = title.length() * titleH * 0.62f;
        float  titleX   = -titleW * 0.5f;
        float  titleY   = top - padY;
        c.addText(title, titleX, titleY, titleH, 1.0f, 1.0f, 0.55f, 1.0f);

        // Menu items
        float itemY = titleY - titleH - titleGap;
        for (int i = 0; i < LABELS.length; i++) {
            boolean sel   = (i == selected.ordinal());
            String  label = (sel ? "> " : "  ") + LABELS[i];
            float   lw    = label.length() * cellW;
            float   lx    = -lw * 0.5f;

            if (sel) {
                // Highlight that tightly wraps the text row.
                // Text occupies itemY (bottom) → itemY + charH (top); add a small pad.
                float hlPad = charH * 0.2f;
                c.addQuad(lx - padX * 0.4f, itemY + charH + hlPad,
                          lw + padX * 0.8f, charH + hlPad * 2,
                          -0.001f, 0.35f, 0.55f, 0.95f, 0.30f);
                c.addText(label, lx, itemY, charH, 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                c.addText(label, lx, itemY, charH, 0.65f, 0.75f, 0.85f, 0.85f);
            }
            itemY -= lineH;
        }

        return new RenderChunkMesh(CHUNK, new DebugMesh(c.vertices()));
    }
}
