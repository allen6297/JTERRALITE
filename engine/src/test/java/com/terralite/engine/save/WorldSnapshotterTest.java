package com.terralite.engine.save;

import com.terralite.engine.chunk.Chunk;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.Entity;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;
import com.terralite.engine.terrain.CompactBlockStorage;
import com.terralite.engine.terrain.MultiblockBlockStorage;
import com.terralite.engine.terrain.SparseBlockStorage;
import com.terralite.engine.world.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorldSnapshotterTest {
    @TempDir
    Path tempDir;

    @Test
    void snapshotCapturesChunkPositionsAndEntityIdsInWorldOrder() {
        World world = new World();
        world.putChunk(new Chunk(ChunkPos.of(0, 0, 0)));
        world.putChunk(new Chunk(ChunkPos.of(1, 0, 0)));
        Entity first = world.entities().create();
        Entity second = world.entities().create();

        WorldSnapshot snapshot = new WorldSnapshotter().snapshot(world);

        assertEquals(List.of(ChunkPos.of(0, 0, 0), ChunkPos.of(1, 0, 0)), snapshot.chunks());
        assertEquals(List.of(new EntitySnapshot(first.id()), new EntitySnapshot(second.id())), snapshot.entities());
    }

    @Test
    void snapshotCapturesOriginBlocksAndOptionalStateIds() {
        BlockState stone = BlockState.of("terralite:stone");
        World world = new World(new CompactBlockStorage(
                state -> {
                    if (state.equals(BlockState.AIR)) {
                        return 0;
                    }
                    if (state.equals(stone)) {
                        return 4;
                    }
                    throw new IllegalArgumentException("Unknown state");
                },
                id -> id == 0 ? BlockState.AIR : stone,
                0
        ));
        world.setBlock(BlockPos.of(1, 2, 3), stone);

        WorldSnapshot snapshot = new WorldSnapshotter().snapshot(world);

        assertEquals(List.of(new BlockSnapshot(BlockPos.of(1, 2, 3), stone, 4)), snapshot.blocks());
        assertEquals(4, snapshot.blocks().getFirst().stateIdOptional().orElseThrow());
    }

    @Test
    void snapshotOnlyCapturesMultiblockOriginsAndRestoreRebuildsChildren() {
        BlockState doubleStone = BlockState.of("terralite:double_stone");
        World world = new World(multiblockStorage());
        world.setBlock(BlockPos.of(2, 0, 0), doubleStone);

        WorldSnapshot snapshot = new WorldSnapshotter().snapshot(world);
        World restored = new WorldSnapshotter().restore(snapshot, multiblockStorage());

        assertEquals(List.of(new BlockSnapshot(BlockPos.of(2, 0, 0), doubleStone, null)), snapshot.blocks());
        assertEquals(doubleStone, restored.getBlock(BlockPos.of(2, 0, 0)));
        assertEquals(doubleStone, restored.getBlock(BlockPos.of(3, 0, 0)));
        assertEquals(List.of(BlockPos.of(2, 0, 0), BlockPos.of(3, 0, 0)),
                List.copyOf(restored.collisionBlockPositions()));
    }

    @Test
    void restoreRecreatesChunksBlocksAndEntityIds() {
        BlockState stone = BlockState.of("terralite:stone");
        WorldSnapshot snapshot = new WorldSnapshot(
                List.of(ChunkPos.of(0, 0, 0)),
                List.of(new EntitySnapshot(EntityId.of(42))),
                List.of(new BlockSnapshot(BlockPos.of(1, 2, 3), stone, null))
        );

        World restored = new WorldSnapshotter().restore(snapshot);

        assertEquals(List.of(ChunkPos.of(0, 0, 0)), List.copyOf(restored.chunkPositions()));
        assertEquals(stone, restored.getBlock(BlockPos.of(1, 2, 3)));
        assertEquals(EntityId.of(42), restored.entities().require(EntityId.of(42)).id());
    }

    @Test
    void jsonCodecRoundTripsSnapshotData() throws Exception {
        WorldSnapshot snapshot = new WorldSnapshot(
                List.of(ChunkPos.of(1, 2, 3)),
                List.of(new EntitySnapshot(EntityId.of(42))),
                List.of(new BlockSnapshot(
                        BlockPos.of(4, 5, 6),
                        BlockState.of("terralite:crops/wheat").with("age", "7"),
                        12
                ))
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        WorldSnapshotJsonCodec codec = new WorldSnapshotJsonCodec();

        codec.write(snapshot, output);
        WorldSnapshot decoded = codec.read(new ByteArrayInputStream(output.toByteArray()));

        assertEquals(snapshot, decoded);
        assertEquals(12, decoded.blocks().getFirst().stateIdOptional().orElseThrow());
        assertEquals("7", decoded.blocks().getFirst().state().property("age"));
    }

    @Test
    void jsonCodecReadsAndWritesSnapshotFiles() throws Exception {
        WorldSnapshot snapshot = new WorldSnapshot(
                List.of(ChunkPos.of(0, 0, 0)),
                List.of(),
                List.of(new BlockSnapshot(BlockPos.of(1, 2, 3), BlockState.of("terralite:stone"), null))
        );
        Path path = tempDir.resolve("world.json");
        WorldSnapshotJsonCodec codec = new WorldSnapshotJsonCodec();

        codec.write(snapshot, path);
        WorldSnapshot decoded = codec.read(path);

        assertEquals(snapshot, decoded);
        String json = Files.readString(path);
        assertEquals(true, json.contains("\"chunks\""));
        assertEquals(true, json.contains("\"blocks\""));
        assertEquals(true, json.contains("\"terralite:stone\""));
    }

    @Test
    void snapshotDefensivelyCopiesLists() {
        List<ChunkPos> chunks = new ArrayList<>();
        List<EntitySnapshot> entities = new ArrayList<>();
        List<BlockSnapshot> blocks = new ArrayList<>();
        chunks.add(ChunkPos.of(0, 0, 0));
        entities.add(new EntitySnapshot(EntityId.of(1)));
        blocks.add(new BlockSnapshot(BlockPos.of(1, 2, 3), BlockState.of("terralite:stone"), null));

        WorldSnapshot snapshot = new WorldSnapshot(chunks, entities, blocks);
        chunks.clear();
        entities.clear();
        blocks.clear();

        assertEquals(List.of(ChunkPos.of(0, 0, 0)), snapshot.chunks());
        assertEquals(List.of(new EntitySnapshot(EntityId.of(1))), snapshot.entities());
        assertEquals(List.of(new BlockSnapshot(BlockPos.of(1, 2, 3), BlockState.of("terralite:stone"), null)),
                snapshot.blocks());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.chunks().add(ChunkPos.of(1, 0, 0)));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.entities().add(new EntitySnapshot(EntityId.of(2))));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.blocks().add(new BlockSnapshot(BlockPos.of(4, 5, 6), BlockState.of("terralite:dirt"), null)));
    }

    private static MultiblockBlockStorage multiblockStorage() {
        return new MultiblockBlockStorage(new SparseBlockStorage(), state -> {
            if (state.id().toString().equals("terralite:double_stone")) {
                return List.of(BlockPos.of(0, 0, 0), BlockPos.of(1, 0, 0));
            }
            return List.of(BlockPos.of(0, 0, 0));
        });
    }
}
