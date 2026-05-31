package com.terralite.render.texture;

import com.terralite.core.registry.ResourceId;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextureAtlasBuilderTest {
    @TempDir
    Path tempDir;

    @Test
    void buildsAtlasAndRemapsMeshUvsIntoTextureRegion() throws Exception {
        ResourceId first = ResourceId.id("terralite:block/first");
        ResourceId second = ResourceId.id("terralite:block/second");
        Path firstPath = texture("first.png", Color.RED);
        Path secondPath = texture("second.png", Color.BLUE);

        TextureAtlas atlas = new TextureAtlasBuilder().build(Map.of(first, firstPath, second, secondPath));
        DebugMesh mesh = new DebugMesh(List.of(
                new DebugVertex(0, 0, 0, 1, 1, 1, 0, 0, second),
                new DebugVertex(1, 0, 0, 1, 1, 1, 1, 0, second),
                new DebugVertex(0, 1, 0, 1, 1, 1, 0, 1, second)
        ));

        DebugMesh remapped = new TextureAtlasMapper().remap(mesh, atlas);
        TextureRegion region = atlas.region(second).orElseThrow();

        assertEquals(4, atlas.width());
        assertEquals(2, atlas.height());
        assertEquals(region.u0(), remapped.vertices().get(0).u());
        assertEquals(region.u1(), remapped.vertices().get(1).u());
        assertEquals(region.v1(), remapped.vertices().get(2).v());
    }

    private Path texture(String name, Color color) throws Exception {
        Path path = tempDir.resolve(name);
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        ImageIO.write(image, "png", path.toFile());
        return path;
    }
}
