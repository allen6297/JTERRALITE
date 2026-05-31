package com.terralite.tools.render;

import com.terralite.content.assets.ContentAssetIndex;
import com.terralite.content.assets.ContentModelFormat;
import com.terralite.content.assets.ContentModelIndex;
import com.terralite.content.assets.model.ContentModelMeshLoader;
import com.terralite.content.assets.model.ContentModelMeshLibrary;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.camera.Camera;
import com.terralite.engine.physics.Transform;
import com.terralite.game.content.GameContentLoader;
import com.terralite.game.registry.TerraliteRegistries;
import com.terralite.render.RenderChunk;
import com.terralite.render.texture.TextureAtlasBuilder;
import com.terralite.runtime.render.RenderSceneExtractor;
import com.terralite.runtime.world.RuntimeWorldFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentRenderSmokeTest {
    @Test
    void realPackContentCreatesRenderableChunkScene() throws Exception {
        var content = new GameContentLoader().load(repoPacksRoot());
        var assets = ContentAssetIndex.load(content.packs());
        var models = ContentModelIndex.load(content.packs());
        var modelMeshes = new ContentModelMeshLibrary().loadSupported(models);
        var textureAtlas = new TextureAtlasBuilder().build(assets.assets().stream()
                .filter(asset -> asset.type().equals("textures"))
                .collect(Collectors.toMap(asset -> asset.id(), asset -> asset.path())));
        var world = new RuntimeWorldFactory().create(content.gameData());
        var camera = new Camera(
                new Transform(0.0, 4.0, 10.0),
                70.0,
                0.1,
                1000.0,
                0.0,
                -15.0
        );

        var scene = RenderSceneExtractor.from(world, camera, content.gameData(), modelMeshes);

        assertEquals(List.of(
                new RenderChunk(-1, 0, -1),
                new RenderChunk(-1, 0, 0),
                new RenderChunk(-1, 0, 1),
                new RenderChunk(0, 0, -1),
                new RenderChunk(0, 0, 0),
                new RenderChunk(0, 0, 1),
                new RenderChunk(1, 0, -1),
                new RenderChunk(1, 0, 0),
                new RenderChunk(1, 0, 1)
        ), scene.chunks());
        assertEquals(9, scene.chunkMeshes().size());
        assertFalse(scene.chunkMeshes().get(0).mesh().vertices().isEmpty());
        assertTrue(scene.chunkMeshes().stream()
                .flatMap(chunkMesh -> chunkMesh.mesh().vertices().stream())
                .anyMatch(vertex -> vertex.texture() != null),
                () -> content.gameData().registry(TerraliteRegistries.BLOCKS).ids().toString());
        assertTrue(assets.findTexture(ResourceId.id("terralite:block/grass_block_top")).isPresent());
        assertTrue(textureAtlas.region(ResourceId.id("terralite:block/grass_block_top")).isPresent());
        assertTrue(assets.findModel(ResourceId.id("terralite:block/cube_column")).isPresent());
        assertEquals(ContentModelFormat.BLOCKBENCH, models.find(ResourceId.id("terralite:block/blockbench_sample")).orElseThrow().format());
        var objModel = models.find(ResourceId.id("terralite:block/obj_sample")).orElseThrow();
        assertEquals(ContentModelFormat.WAVEFRONT_OBJ, objModel.format());
        assertEquals(1, new ContentModelMeshLoader().load(objModel).triangleCount());
        assertEquals(5, modelMeshes.size());
    }

    private static Path repoPacksRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path direct = workingDirectory.resolve("packs");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return workingDirectory.resolve("../packs").normalize();
    }
}
