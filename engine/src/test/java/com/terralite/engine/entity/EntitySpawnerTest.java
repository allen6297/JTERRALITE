package com.terralite.engine.entity;

import com.terralite.engine.physics.PhysicsComponents;
import com.terralite.engine.physics.Transform;
import com.terralite.engine.physics.Velocity;
import com.terralite.engine.player.PlayerComponents;
import com.terralite.engine.player.PlayerControlled;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitySpawnerTest {
    @Test
    void spawnerCreatesEntityWithRequestedComponents() {
        World world = new World();
        EntitySpawner spawner = new EntitySpawner(world);
        Transform transform = new Transform(1.0, 2.0, 3.0);
        Velocity velocity = new Velocity(4.0, 5.0, 6.0);
        PlayerControlled player = new PlayerControlled(8.0);

        Entity entity = spawner.spawn(EntitySpawnRequest.create()
            .transform(transform)
            .velocity(velocity)
            .playerControlled(player));

        assertSame(entity, world.entities().require(entity.id()));
        assertEquals(transform, entity.require(PhysicsComponents.TRANSFORM));
        assertEquals(velocity, entity.require(PhysicsComponents.VELOCITY));
        assertEquals(player, entity.require(PlayerComponents.PLAYER_CONTROLLED));
    }

    @Test
    void spawnerCanCreateEntityWithoutComponents() {
        World world = new World();

        Entity entity = world.spawner().spawn(EntitySpawnRequest.create());

        assertSame(entity, world.entities().require(entity.id()));
        assertEquals(0, entity.componentCount());
    }

    @Test
    void spawnerDespawnsExistingEntities() {
        World world = new World();
        EntitySpawner spawner = world.spawner();
        Entity entity = spawner.spawn(EntitySpawnRequest.create());

        assertSame(entity, spawner.despawn(entity.id()));

        assertFalse(world.entities().contains(entity.id()));
        assertThrows(IllegalArgumentException.class, () -> spawner.despawn(entity.id()));
    }

    @Test
    void spawnRequestExposesRequestedValues() {
        Transform transform = Transform.ORIGIN;
        Velocity velocity = Velocity.ZERO;
        PlayerControlled player = new PlayerControlled(1.0);

        EntitySpawnRequest request = EntitySpawnRequest.create()
            .transform(transform)
            .velocity(velocity)
            .playerControlled(player);

        assertEquals(transform, request.transform().orElseThrow());
        assertEquals(velocity, request.velocity().orElseThrow());
        assertEquals(player, request.playerControlled().orElseThrow());
    }

    @Test
    void emptySpawnRequestHasNoComponentValues() {
        EntitySpawnRequest request = EntitySpawnRequest.create();

        assertTrue(request.transform().isEmpty());
        assertTrue(request.velocity().isEmpty());
        assertTrue(request.playerControlled().isEmpty());
    }
}
