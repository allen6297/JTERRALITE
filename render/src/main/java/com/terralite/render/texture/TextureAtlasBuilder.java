package com.terralite.render.texture;

import com.terralite.core.registry.ResourceId;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TextureAtlasBuilder {
    public TextureAtlas build(Map<ResourceId, Path> textures) throws IOException {
        return build(textures, Map.of());
    }

    public TextureAtlas build(Map<ResourceId, Path> textures, Map<ResourceId, BufferedImage> extra) throws IOException {
        Objects.requireNonNull(textures, "textures");
        Objects.requireNonNull(extra, "extra");

        Map<ResourceId, BufferedImage> images = new LinkedHashMap<>();
        BufferedImage white = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        white.setRGB(0, 0, 0xffffffff);
        images.put(ResourceId.id("terralite:ui/white"), white);
        images.put(TextureAtlas.FONT_ASCII, fontAtlasImage());

        // extra images act as fallbacks; pack textures loaded afterwards override them
        images.putAll(extra);

        int width = 0;
        int height = 0;
        for (var entry : textures.entrySet()) {
            BufferedImage image = ImageIO.read(entry.getValue().toFile());
            if (image == null) {
                throw new IOException("Unsupported texture image: " + entry.getValue());
            }
            images.put(entry.getKey(), image); // pack wins over extra
        }
        for (BufferedImage image : images.values()) {
            width += image.getWidth();
            height = Math.max(height, image.getHeight());
        }

        int[] pixels = new int[width * height];
        Map<ResourceId, TextureRegion> regions = new LinkedHashMap<>();
        int offsetX = 0;
        for (var entry : images.entrySet()) {
            BufferedImage image = entry.getValue();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int sourceX = x;
                    int sourceY = y;
                    pixels[y * width + offsetX + x] = image.getRGB(sourceX, sourceY);
                }
            }
            regions.put(entry.getKey(), new TextureRegion(
                    offsetX / (float) width,
                    0.0f,
                    (offsetX + image.getWidth()) / (float) width,
                    image.getHeight() / (float) height
            ));
            offsetX += image.getWidth();
        }

        return new TextureAtlas(width, height, pixels, regions);
    }

    private static BufferedImage fontAtlasImage() {
        int glyphWidth = 16;
        int glyphHeight = 20;
        int columns = 16;
        int rows = 6;
        BufferedImage image = new BufferedImage(glyphWidth * columns, glyphHeight * rows, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setComposite(java.awt.AlphaComposite.Clear);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setComposite(java.awt.AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
            for (int code = 32; code <= 126; code++) {
                int index = code - 32;
                int col = index % columns;
                int row = index / columns;
                g.drawString(Character.toString((char) code), col * glyphWidth + 2, row * glyphHeight + 16);
            }
        } finally {
            g.dispose();
        }
        return image;
    }
}
