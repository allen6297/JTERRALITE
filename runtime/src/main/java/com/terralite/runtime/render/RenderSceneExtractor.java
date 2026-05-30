package com.terralite.runtime.render;

import com.terralite.engine.camera.Camera;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.world.World;
import com.terralite.render.RenderCamera;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderObject;
import com.terralite.render.RenderScene;

import java.util.Comparator;
import java.util.Objects;

public final class RenderSceneExtractor {
    private RenderSceneExtractor() {
    }

    public static RenderScene from(World world, Camera camera) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(camera, "camera");

        RenderScene.Builder scene = RenderScene.builder()
                .camera(toRenderCamera(camera));

        world.chunkPositions().stream()
                .sorted(Comparator
                        .comparingInt(ChunkPos::x)
                        .thenComparingInt(ChunkPos::y)
                        .thenComparingInt(ChunkPos::z))
                .map(pos -> new RenderChunk(pos.x(), pos.y(), pos.z()))
                .forEach(scene::addChunk);

        world.entities().entities().stream()
                .sorted(Comparator.comparingLong(entity -> entity.id().value()))
                .forEach(entity -> addEntityObject(scene, entity));

        return scene.build();
    }

    private static RenderCamera toRenderCamera(Camera camera) {
        Transform transform = camera.transform();
        return new RenderCamera(
                transform.x(),
                transform.y(),
                transform.z(),
                camera.fovDegrees(),
                camera.nearPlane(),
                camera.farPlane(),
                camera.yaw(),
                camera.pitch()
        );
    }

    private static void addEntityObject(RenderScene.Builder scene, Entity entity) {
        entity.get(PhysicsComponents.TRANSFORM)
                .map(transform -> RenderObject.of(
                        "terralite:entity/" + entity.id().value(),
                        transform.x(),
                        transform.y(),
                        transform.z()
                ))
                .ifPresent(scene::addObject);
    }
}
