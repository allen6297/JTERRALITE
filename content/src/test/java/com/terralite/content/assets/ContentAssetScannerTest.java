package com.terralite.content.assets;

import com.terralite.content.pack.ContentPack;
import com.terralite.content.pack.ContentPackLoader;
import com.terralite.core.registry.ResourceId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentAssetScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void scansTextureAndModelAssetsByTypeAndResourceId() throws Exception {
        Path packRoot = tempDir.resolve("base");
        Files.createDirectories(packRoot.resolve("assets/textures/block"));
        Files.createDirectories(packRoot.resolve("assets/models/block"));
        Files.writeString(packRoot.resolve("pack.json"), """
            {
              "id": "terralite:base",
              "name": "Terralite Base",
              "version": "1.0.0"
            }
            """);
        Files.write(packRoot.resolve("assets/textures/block/stone.png"), new byte[] {1, 2, 3});
        Files.writeString(packRoot.resolve("assets/models/block/cube_all.json"), "{}");
        Files.writeString(packRoot.resolve("assets/models/block/custom_rock.bbmodel"), "{}");
        Files.writeString(packRoot.resolve("assets/models/block/custom_statue.obj"), "o custom_statue\n");

        ContentPack pack = new ContentPackLoader().load(packRoot);
        List<ContentAsset> assets = new ContentAssetScanner().scan(pack);
        ContentAssetIndex index = ContentAssetIndex.load(List.of(pack));
        ContentModelIndex models = ContentModelIndex.load(List.of(pack));

        assertEquals(List.of(
                new ContentAsset("models", ResourceId.id("terralite:block/cube_all"), packRoot.resolve("assets/models/block/cube_all.json")),
                new ContentAsset("models", ResourceId.id("terralite:block/custom_rock"), packRoot.resolve("assets/models/block/custom_rock.bbmodel")),
                new ContentAsset("models", ResourceId.id("terralite:block/custom_statue"), packRoot.resolve("assets/models/block/custom_statue.obj")),
                new ContentAsset("textures", ResourceId.id("terralite:block/stone"), packRoot.resolve("assets/textures/block/stone.png"))
        ), assets);
        assertEquals(4, index.size());
        assertTrue(index.findTexture(ResourceId.id("terralite:block/stone")).isPresent());
        assertTrue(index.findModel(ResourceId.id("terralite:block/cube_all")).isPresent());
        assertEquals(3, models.size());
        assertEquals(ContentModelFormat.TERRALITE_JSON, models.find(ResourceId.id("terralite:block/cube_all")).orElseThrow().format());
        assertEquals(ContentModelFormat.BLOCKBENCH, models.find(ResourceId.id("terralite:block/custom_rock")).orElseThrow().format());
        assertEquals(ContentModelFormat.WAVEFRONT_OBJ, models.find(ResourceId.id("terralite:block/custom_statue")).orElseThrow().format());
    }
}
