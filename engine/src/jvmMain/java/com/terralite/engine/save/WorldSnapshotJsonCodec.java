package com.terralite.engine.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.terralite.core.registry.ResourceId;
import com.terralite.engine.chunk.ChunkPos;
import com.terralite.engine.entity.EntityId;
import com.terralite.engine.terrain.BlockPos;
import com.terralite.engine.terrain.BlockState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WorldSnapshotJsonCodec {
    private final ObjectMapper mapper;

    public WorldSnapshotJsonCodec() {
        this(new ObjectMapper().findAndRegisterModules()
                .enable(SerializationFeature.INDENT_OUTPUT));
    }

    public WorldSnapshotJsonCodec(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public void write(WorldSnapshot snapshot, OutputStream output) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(output, "output");
        mapper.writeValue(output, SnapshotJson.from(snapshot));
    }

    public void write(WorldSnapshot snapshot, Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (OutputStream output = Files.newOutputStream(path)) {
            write(snapshot, output);
        }
    }

    public WorldSnapshot read(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return mapper.readValue(input, SnapshotJson.class).toSnapshot();
    }

    public WorldSnapshot read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream input = Files.newInputStream(path)) {
            return read(input);
        }
    }

    private record SnapshotJson(List<ChunkJson> chunks, List<EntityJson> entities, List<BlockJson> blocks) {
        private SnapshotJson {
            chunks = chunks == null ? List.of() : List.copyOf(chunks);
            entities = entities == null ? List.of() : List.copyOf(entities);
            blocks = blocks == null ? List.of() : List.copyOf(blocks);
        }

        private static SnapshotJson from(WorldSnapshot snapshot) {
            return new SnapshotJson(
                    snapshot.chunks().stream().map(ChunkJson::from).toList(),
                    snapshot.entities().stream().map(EntityJson::from).toList(),
                    snapshot.blocks().stream().map(BlockJson::from).toList()
            );
        }

        private WorldSnapshot toSnapshot() {
            return new WorldSnapshot(
                    chunks.stream().map(ChunkJson::toChunkPos).toList(),
                    entities.stream().map(EntityJson::toEntitySnapshot).toList(),
                    blocks.stream().map(BlockJson::toBlockSnapshot).toList()
            );
        }
    }

    private record ChunkJson(int x, int y, int z) {
        private static ChunkJson from(ChunkPos pos) {
            return new ChunkJson(pos.x(), pos.y(), pos.z());
        }

        private ChunkPos toChunkPos() {
            return ChunkPos.of(x, y, z);
        }
    }

    private record EntityJson(long id) {
        private static EntityJson from(EntitySnapshot snapshot) {
            return new EntityJson(snapshot.id().value());
        }

        private EntitySnapshot toEntitySnapshot() {
            return new EntitySnapshot(EntityId.of(id));
        }
    }

    private record BlockJson(int x, int y, int z, String id, Map<String, String> properties, Integer stateId) {
        private BlockJson {
            Objects.requireNonNull(id, "id");
            properties = properties == null ? Map.of() : Map.copyOf(properties);
        }

        private static BlockJson from(BlockSnapshot snapshot) {
            return new BlockJson(
                    snapshot.pos().x(),
                    snapshot.pos().y(),
                    snapshot.pos().z(),
                    snapshot.state().id().toString(),
                    snapshot.state().properties(),
                    snapshot.stateId()
            );
        }

        private BlockSnapshot toBlockSnapshot() {
            return new BlockSnapshot(
                    BlockPos.of(x, y, z),
                    new BlockState(ResourceId.id(id), properties),
                    stateId
            );
        }
    }
}
