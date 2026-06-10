package com.terralite.runtime.render;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.physics.Transform;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.Viewport;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;
import com.terralite.render.texture.TextureAtlas;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class InspectionTooltipBuilder {
    private static final RenderChunk TOOLTIP_CHUNK = new RenderChunk(Integer.MIN_VALUE + 1, 0, 0);
    private static final int MAX_LINES = 5;
    private static final int MAX_CHARS = 34;
    private static final int FONT_COLUMNS = 16;
    private static final int FONT_ROWS = 6;

    private InspectionTooltipBuilder() {
    }

    public static Optional<RenderChunkMesh> build(Camera camera, Viewport viewport, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Optional.empty();
        }

        Basis basis = Basis.from(camera, viewport);
        List<String> clipped = lines.stream()
                .limit(MAX_LINES)
                .map(InspectionTooltipBuilder::clip)
                .toList();

        float charHeight = basis.viewHeight() * 0.047f;
        float cellWidth = charHeight * 0.62f;
        float lineHeight = charHeight * 1.35f;
        float padX = charHeight * 0.85f;
        float padY = charHeight * 0.75f;

        int maxChars = clipped.stream().mapToInt(String::length).max().orElse(1);
        float panelWidth = padX * 2.0f + maxChars * cellWidth;
        float panelHeight = padY * 2.0f + clipped.size() * lineHeight;

        float left = -basis.viewWidth() * 0.43f;
        float top = basis.viewHeight() * 0.36f;
        List<DebugVertex> vertices = new ArrayList<>();
        addQuad(vertices, basis, left, top, panelWidth, panelHeight, 0.0f, 0.0f, 0.0f, 0.0f, 0.92f);
        addQuad(vertices, basis, left, top, charHeight * 0.24f, panelHeight, -0.001f, 0.4f, 1.0f, 0.75f, 1.0f);

        float textX = left + padX;
        float textY = top - padY - charHeight;
        for (int line = 0; line < clipped.size(); line++) {
            float r = line == 0 ? 0.95f : 1.0f;
            float g = line == 0 ? 1.0f : 1.0f;
            float b = line == 0 ? 0.78f : 1.0f;
            addText(vertices, basis, clipped.get(line), textX, textY - line * lineHeight,
                    charHeight, r, g, b, 0.98f);
        }

        return Optional.of(new RenderChunkMesh(TOOLTIP_CHUNK, new DebugMesh(vertices)));
    }

    private static String clip(String line) {
        String normalized = line == null ? "" : line.toUpperCase(Locale.ROOT);
        return normalized.length() <= MAX_CHARS ? normalized : normalized.substring(0, MAX_CHARS - 3) + "...";
    }

    private static void addText(
            List<DebugVertex> vertices,
            Basis basis,
            String text,
            float x,
            float baselineY,
            float charHeight,
            float r,
            float g,
            float b,
            float a
    ) {
        float cellWidth = charHeight * 0.62f;
        float cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            addGlyphQuad(vertices, basis, text.charAt(i), cursorX, baselineY + charHeight,
                    charHeight * 0.58f, charHeight, -0.003f, r, g, b, a);
            cursorX += cellWidth;
        }
    }

    private static void addGlyphQuad(
            List<DebugVertex> vertices,
            Basis basis,
            char c,
            float left,
            float top,
            float width,
            float height,
            float depthOffset,
            float r,
            float g,
            float b,
            float a
    ) {
        int code = Math.max(32, Math.min(126, c));
        int index = code - 32;
        int col = index % FONT_COLUMNS;
        int row = index / FONT_COLUMNS;
        float u0 = col / (float) FONT_COLUMNS;
        float v0 = row / (float) FONT_ROWS;
        float u1 = (col + 1) / (float) FONT_COLUMNS;
        float v1 = (row + 1) / (float) FONT_ROWS;

        float right = left + width;
        float bottom = top - height;
        addTexturedVertex(vertices, basis, left, bottom, depthOffset, r, g, b, a, u0, v1);
        addTexturedVertex(vertices, basis, right, bottom, depthOffset, r, g, b, a, u1, v1);
        addTexturedVertex(vertices, basis, right, top, depthOffset, r, g, b, a, u1, v0);
        addTexturedVertex(vertices, basis, left, bottom, depthOffset, r, g, b, a, u0, v1);
        addTexturedVertex(vertices, basis, right, top, depthOffset, r, g, b, a, u1, v0);
        addTexturedVertex(vertices, basis, left, top, depthOffset, r, g, b, a, u0, v0);
    }

    private static void addQuad(
            List<DebugVertex> vertices,
            Basis basis,
            float left,
            float top,
            float width,
            float height,
            float depthOffset,
            float r,
            float g,
            float b,
            float a
    ) {
        float right = left + width;
        float bottom = top - height;
        addVertex(vertices, basis, left, bottom, depthOffset, r, g, b, a);
        addVertex(vertices, basis, right, bottom, depthOffset, r, g, b, a);
        addVertex(vertices, basis, right, top, depthOffset, r, g, b, a);
        addVertex(vertices, basis, left, bottom, depthOffset, r, g, b, a);
        addVertex(vertices, basis, right, top, depthOffset, r, g, b, a);
        addVertex(vertices, basis, left, top, depthOffset, r, g, b, a);
    }

    private static void addVertex(List<DebugVertex> vertices, Basis basis, float x, float y, float depthOffset,
                                  float r, float g, float b, float a) {
        float wx = basis.centerX() + basis.rightX() * x + basis.upX() * y + basis.forwardX() * depthOffset;
        float wy = basis.centerY() + basis.rightY() * x + basis.upY() * y + basis.forwardY() * depthOffset;
        float wz = basis.centerZ() + basis.rightZ() * x + basis.upZ() * y + basis.forwardZ() * depthOffset;
        vertices.add(new DebugVertex(wx, wy, wz, r, g, b, a, 0.0f, 0.0f, null));
    }

    private static void addTexturedVertex(List<DebugVertex> vertices, Basis basis, float x, float y, float depthOffset,
                                          float r, float g, float b, float a, float u, float v) {
        float wx = basis.centerX() + basis.rightX() * x + basis.upX() * y + basis.forwardX() * depthOffset;
        float wy = basis.centerY() + basis.rightY() * x + basis.upY() * y + basis.forwardY() * depthOffset;
        float wz = basis.centerZ() + basis.rightZ() * x + basis.upZ() * y + basis.forwardZ() * depthOffset;
        vertices.add(new DebugVertex(wx, wy, wz, r, g, b, a, u, v, TextureAtlas.FONT_ASCII));
    }

    private static String[] glyph(char c) {
        return switch (c) {
            case 'A' -> g(" ### ","#   #","#   #","#####","#   #","#   #","#   #");
            case 'B' -> g("#### ","#   #","#   #","#### ","#   #","#   #","#### ");
            case 'C' -> g(" ####","#    ","#    ","#    ","#    ","#    "," ####");
            case 'D' -> g("#### ","#   #","#   #","#   #","#   #","#   #","#### ");
            case 'E' -> g("#####","#    ","#    ","#### ","#    ","#    ","#####");
            case 'F' -> g("#####","#    ","#    ","#### ","#    ","#    ","#    ");
            case 'G' -> g(" ####","#    ","#    ","#  ##","#   #","#   #"," ####");
            case 'H' -> g("#   #","#   #","#   #","#####","#   #","#   #","#   #");
            case 'I' -> g("#####","  #  ","  #  ","  #  ","  #  ","  #  ","#####");
            case 'J' -> g("#####","   # ","   # ","   # ","   # ","#  # "," ##  ");
            case 'K' -> g("#   #","#  # ","# #  ","##   ","# #  ","#  # ","#   #");
            case 'L' -> g("#    ","#    ","#    ","#    ","#    ","#    ","#####");
            case 'M' -> g("#   #","## ##","# # #","#   #","#   #","#   #","#   #");
            case 'N' -> g("#   #","##  #","# # #","#  ##","#   #","#   #","#   #");
            case 'O' -> g(" ### ","#   #","#   #","#   #","#   #","#   #"," ### ");
            case 'P' -> g("#### ","#   #","#   #","#### ","#    ","#    ","#    ");
            case 'Q' -> g(" ### ","#   #","#   #","#   #","# # #","#  # "," ## #");
            case 'R' -> g("#### ","#   #","#   #","#### ","# #  ","#  # ","#   #");
            case 'S' -> g(" ####","#    ","#    "," ### ","    #","    #","#### ");
            case 'T' -> g("#####","  #  ","  #  ","  #  ","  #  ","  #  ","  #  ");
            case 'U' -> g("#   #","#   #","#   #","#   #","#   #","#   #"," ### ");
            case 'V' -> g("#   #","#   #","#   #","#   #"," # # "," # # ","  #  ");
            case 'W' -> g("#   #","#   #","#   #","# # #","# # #","## ##","#   #");
            case 'X' -> g("#   #"," # # "," # # ","  #  "," # # "," # # ","#   #");
            case 'Y' -> g("#   #"," # # "," # # ","  #  ","  #  ","  #  ","  #  ");
            case 'Z' -> g("#####","    #","   # ","  #  "," #   ","#    ","#####");
            case '0' -> g(" ### ","#   #","#  ##","# # #","##  #","#   #"," ### ");
            case '1' -> g("  #  "," ##  ","  #  ","  #  ","  #  ","  #  "," ### ");
            case '2' -> g(" ### ","#   #","    #","   # ","  #  "," #   ","#####");
            case '3' -> g("#### ","    #","    #"," ### ","    #","    #","#### ");
            case '4' -> g("#   #","#   #","#   #","#####","    #","    #","    #");
            case '5' -> g("#####","#    ","#    ","#### ","    #","    #","#### ");
            case '6' -> g(" ####","#    ","#    ","#### ","#   #","#   #"," ### ");
            case '7' -> g("#####","    #","   # ","  #  "," #   "," #   "," #   ");
            case '8' -> g(" ### ","#   #","#   #"," ### ","#   #","#   #"," ### ");
            case '9' -> g(" ### ","#   #","#   #"," ####","    #","    #"," ### ");
            case ':' -> g("     ","  #  ","  #  ","     ","  #  ","  #  ","     ");
            case '/' -> g("    #","    #","   # ","  #  "," #   ","#    ","#    ");
            case '-' -> g("     ","     ","     ","#####","     ","     ","     ");
            case '_' -> g("     ","     ","     ","     ","     ","     ","#####");
            case '.' -> g("     ","     ","     ","     ","     "," ##  "," ##  ");
            case ',' -> g("     ","     ","     ","     "," ##  "," ##  "," #   ");
            case '@' -> g(" ### ","#   #","# ###","# # #","# ###","#    "," ####");
            case '[' -> g(" ### "," #   "," #   "," #   "," #   "," #   "," ### ");
            case ']' -> g(" ### ","   # ","   # ","   # ","   # ","   # "," ### ");
            case '{' -> g("  ## "," #   "," #   ","##   "," #   "," #   ","  ## ");
            case '}' -> g(" ##  ","   # ","   # ","   ##","   # ","   # "," ##  ");
            case ' ' -> g("     ","     ","     ","     ","     ","     ","     ");
            default -> g("#####","#   #","   # ","  #  ","     ","  #  ","     ");
        };
    }

    private static String[] g(String... rows) {
        return rows;
    }

    private record Basis(
            float centerX, float centerY, float centerZ,
            float forwardX, float forwardY, float forwardZ,
            float rightX, float rightY, float rightZ,
            float upX, float upY, float upZ,
            float viewWidth, float viewHeight
    ) {
        private static Basis from(Camera camera, Viewport viewport) {
            Transform transform = camera.transform();
            double yawRad = Math.toRadians(camera.yaw());
            double pitchRad = Math.toRadians(camera.pitch());

            float forwardX = (float) (-Math.sin(yawRad) * Math.cos(pitchRad));
            float forwardY = (float) Math.sin(pitchRad);
            float forwardZ = (float) (-Math.cos(yawRad) * Math.cos(pitchRad));

            float rightX = (float) Math.cos(yawRad);
            float rightY = 0.0f;
            float rightZ = (float) -Math.sin(yawRad);

            float upX = rightY * forwardZ - rightZ * forwardY;
            float upY = rightZ * forwardX - rightX * forwardZ;
            float upZ = rightX * forwardY - rightY * forwardX;

            float distance = 0.45f;
            float viewHeight = (float) (2.0 * distance * Math.tan(Math.toRadians(camera.fovDegrees()) * 0.5));
            float viewWidth = viewHeight * (float) viewport.aspectRatio();

            return new Basis(
                    (float) transform.x() + forwardX * distance,
                    (float) transform.y() + forwardY * distance,
                    (float) transform.z() + forwardZ * distance,
                    forwardX, forwardY, forwardZ,
                    rightX, rightY, rightZ,
                    upX, upY, upZ,
                    viewWidth, viewHeight
            );
        }
    }
}
