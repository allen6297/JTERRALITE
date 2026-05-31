package com.terralite.render.texture;

import com.terralite.core.registry.ResourceId;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TextureAtlasBuilder {
    public TextureAtlas build(Map<ResourceId, Path> textures) throws IOException {
        Objects.requireNonNull(textures, "textures");
        if (textures.isEmpty()) {
            return new TextureAtlas(1, 1, new int[] {0xffffffff}, Map.of());
        }

        Map<ResourceId, BufferedImage> images = new LinkedHashMap<>();
        int tileWidth = 1;
        int tileHeight = 1;
        for (var entry : textures.entrySet()) {
            BufferedImage image = ImageIO.read(entry.getValue().toFile());
            if (image == null) {
                throw new IOException("Unsupported texture image: " + entry.getValue());
            }
            images.put(entry.getKey(), image);
            tileWidth = Math.max(tileWidth, image.getWidth());
            tileHeight = Math.max(tileHeight, image.getHeight());
        }

        int width = tileWidth * images.size();
        int height = tileHeight;
        int[] pixels = new int[width * height];
        Map<ResourceId, TextureRegion> regions = new LinkedHashMap<>();
        int tile = 0;
        for (var entry : images.entrySet()) {
            int offsetX = tile * tileWidth;
            BufferedImage image = entry.getValue();
            for (int y = 0; y < tileHeight; y++) {
                for (int x = 0; x < tileWidth; x++) {
                    int sourceX = Math.min(x, image.getWidth() - 1);
                    int sourceY = Math.min(y, image.getHeight() - 1);
                    pixels[y * width + offsetX + x] = image.getRGB(sourceX, sourceY);
                }
            }
            regions.put(entry.getKey(), new TextureRegion(
                    offsetX / (float) width,
                    0.0f,
                    (offsetX + tileWidth) / (float) width,
                    1.0f
            ));
            tile++;
        }

        return new TextureAtlas(width, height, pixels, regions);
    }
}
