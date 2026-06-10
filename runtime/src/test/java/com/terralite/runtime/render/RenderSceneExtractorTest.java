package com.terralite.runtime.render;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.world.World;
import com.terralite.render.RenderCamera;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderObject;
import com.terralite.render.RenderScene;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderSceneExtractorTest {
    @Test
    void extractsCameraChunksAndTransformedEntities() {
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(1, 0, 0)));
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        Entity visible = world.entities().create()
                .set(PhysicsComponents.TRANSFORM, new Transform(4.0, 5.0, 6.0));
        world.entities().create();
        Camera camera = new Camera(new Transform(1.0, 2.0, 3.0), 80.0, 0.1, 500.0);

        RenderScene scene = RenderSceneExtractor.from(world, camera);

        assertEquals(new RenderCamera(1.0f, 2.0f, 3.0f, 80.0f, 0.1f, 500.0f), scene.camera());
        assertEquals(List.of(new RenderChunk(0, 0, 0), new RenderChunk(1, 0, 0)), scene.chunks());
        assertEquals(
                List.of(RenderObject.of("terralite:entity/" + visible.id().value(), 4.0, 5.0, 6.0)),
                scene.objects()
        );
    }
}
