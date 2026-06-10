package com.terralite.runtime.render;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.physics.Transform;
import com.terralite.render.Viewport;
import com.terralite.render.mesh.DebugVertex;
import com.terralite.render.texture.TextureAtlas;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates screen-space overlay geometry on a virtual plane placed
 * {@link #DISTANCE} units in front of the camera.
 *
 * <p>All x/y coordinates are in view-space where (0, 0) is the screen centre,
 * +X points right, and +Y points up. The usable area spans
 * [-viewWidth/2, +viewWidth/2] × [-viewHeight/2, +viewHeight/2].
 *
 * <p>Depth offsets: negative values bring geometry closer to the camera,
 * so layered panels should use progressively more-negative depths.
 */
public final class ScreenCanvas {
    private static final float DISTANCE = 0.45f;
    private static final int FONT_COLUMNS = 16;
    private static final int FONT_ROWS = 6;

    // Virtual screen plane basis in world space
    private final float cx, cy, cz;   // centre point
    private final float fx, fy, fz;   // forward (into screen)
    private final float rx, ry, rz;   // right
    private final float ux, uy, uz;   // up

    private final float viewWidth;
    private final float viewHeight;

    private final List<DebugVertex> vertices = new ArrayList<>();

    public ScreenCanvas(Camera camera, Viewport viewport) {
        Transform t = camera.transform();
        double yawRad   = Math.toRadians(camera.yaw());
        double pitchRad = Math.toRadians(camera.pitch());

        fx = (float)(-Math.sin(yawRad) * Math.cos(pitchRad));
        fy = (float)  Math.sin(pitchRad);
        fz = (float)(-Math.cos(yawRad) * Math.cos(pitchRad));

        rx =  (float)  Math.cos(yawRad);
        ry =  0.0f;
        rz =  (float) -Math.sin(yawRad);

        ux = ry * fz - rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;

        viewHeight = (float)(2.0 * DISTANCE * Math.tan(Math.toRadians(camera.fovDegrees()) * 0.5));
        viewWidth  = viewHeight * (float) viewport.aspectRatio();

        cx = (float) t.x() + fx * DISTANCE;
        cy = (float) t.y() + fy * DISTANCE;
        cz = (float) t.z() + fz * DISTANCE;
    }

    public float viewWidth()  { return viewWidth; }
    public float viewHeight() { return viewHeight; }

    // ---- Drawing API --------------------------------------------------------

    /**
     * Adds a solid-colour rectangle.
     *
     * @param left   left edge in view-space
     * @param top    top edge in view-space (Y grows upward)
     * @param width  rectangle width (positive)
     * @param height rectangle height (positive)
     * @param depth  depth offset; negative = closer to camera
     */
    public void addQuad(float left, float top, float width, float height, float depth,
                        float r, float g, float b, float a) {
        float right  = left + width;
        float bottom = top - height;
        addColorVert(left,  bottom, depth, r, g, b, a);
        addColorVert(right, bottom, depth, r, g, b, a);
        addColorVert(right, top,    depth, r, g, b, a);
        addColorVert(left,  bottom, depth, r, g, b, a);
        addColorVert(right, top,    depth, r, g, b, a);
        addColorVert(left,  top,    depth, r, g, b, a);
    }

    /**
     * Adds a line of ASCII text using the built-in bitmap font.
     *
     * @param text      text to render (upper-case renders best)
     * @param x         left edge of the first character
     * @param baselineY top of the character cell (not the ascender)
     * @param charHeight height of each character cell
     */
    public void addText(String text, float x, float baselineY, float charHeight,
                        float r, float g, float b, float a) {
        float cellWidth = charHeight * 0.62f;
        float curX = x;
        for (int i = 0; i < text.length(); i++) {
            addGlyph(text.charAt(i), curX, baselineY + charHeight,
                     charHeight * 0.58f, charHeight, -0.003f, r, g, b, a);
            curX += cellWidth;
        }
    }

    /** Returns a snapshot of all vertices accumulated so far. */
    public List<DebugVertex> vertices() {
        return List.copyOf(vertices);
    }

    // ---- Internal helpers ---------------------------------------------------

    private void addGlyph(char c, float left, float top, float w, float h, float depth,
                          float r, float g, float b, float a) {
        int code  = Math.max(32, Math.min(126, (int) c));
        int index = code - 32;
        int col   = index % FONT_COLUMNS;
        int row   = index / FONT_COLUMNS;
        float u0 = col / (float) FONT_COLUMNS,  u1 = (col + 1) / (float) FONT_COLUMNS;
        float v0 = row / (float) FONT_ROWS,      v1 = (row + 1) / (float) FONT_ROWS;
        float right  = left + w;
        float bottom = top  - h;
        addFontVert(left,  bottom, depth, r, g, b, a, u0, v1);
        addFontVert(right, bottom, depth, r, g, b, a, u1, v1);
        addFontVert(right, top,    depth, r, g, b, a, u1, v0);
        addFontVert(left,  bottom, depth, r, g, b, a, u0, v1);
        addFontVert(right, top,    depth, r, g, b, a, u1, v0);
        addFontVert(left,  top,    depth, r, g, b, a, u0, v0);
    }

    private void addColorVert(float x, float y, float depth,
                              float r, float g, float b, float a) {
        float wx = cx + rx * x + ux * y + fx * depth;
        float wy = cy + ry * x + uy * y + fy * depth;
        float wz = cz + rz * x + uz * y + fz * depth;
        vertices.add(new DebugVertex(wx, wy, wz, r, g, b, a, 0f, 0f, null));
    }

    private void addFontVert(float x, float y, float depth,
                             float r, float g, float b, float a, float u, float v) {
        float wx = cx + rx * x + ux * y + fx * depth;
        float wy = cy + ry * x + uy * y + fy * depth;
        float wz = cz + rz * x + uz * y + fz * depth;
        vertices.add(new DebugVertex(wx, wy, wz, r, g, b, a, u, v, TextureAtlas.FONT_ASCII));
    }
}
