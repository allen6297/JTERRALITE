package com.terralite.content.assets.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BlockbenchModelParser {
    private static final float BLOCKBENCH_UNIT = 16.0f;

    private final ObjectMapper mapper;

    public BlockbenchModelParser() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public BlockbenchModelParser(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public ContentModelMesh parse(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "reader");

        JsonNode elements = mapper.readTree(reader).path("elements");
        if (!elements.isArray()) {
            throw new IllegalArgumentException("Blockbench model must contain an elements array");
        }

        List<ContentModelVertex> vertices = new ArrayList<>();
        for (JsonNode element : elements) {
            Box box = Box.from(element);
            JsonNode faces = element.path("faces");
            for (Face face : Face.values()) {
                JsonNode faceNode = faces.path(face.name().toLowerCase());
                if (!faceNode.isMissingNode()) {
                    addFace(vertices, box, face, uv(faceNode));
                }
            }
        }

        return new ContentModelMesh(vertices);
    }

    private static void addFace(List<ContentModelVertex> vertices, Box box, Face face, float[][] uvs) {
        for (int index : face.indices) {
            float[] corner = face.corners[index];
            float[] uv = uvs[index];
            vertices.add(new ContentModelVertex(
                    box.x(corner[0]),
                    box.y(corner[1]),
                    box.z(corner[2]),
                    uv[0],
                    uv[1]
            ));
        }
    }

    private static float[][] uv(JsonNode face) {
        JsonNode uv = face.path("uv");
        if (!uv.isArray() || uv.size() < 4) {
            return defaultUvs();
        }

        float u0 = (float) uv.get(0).asDouble() / BLOCKBENCH_UNIT;
        float v0 = (float) uv.get(1).asDouble() / BLOCKBENCH_UNIT;
        float u1 = (float) uv.get(2).asDouble() / BLOCKBENCH_UNIT;
        float v1 = (float) uv.get(3).asDouble() / BLOCKBENCH_UNIT;
        return new float[][] {
                {u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}
        };
    }

    private static float[][] defaultUvs() {
        return new float[][] {
                {0, 0}, {1, 0}, {1, 1}, {0, 1}
        };
    }

    private record Box(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        private static Box from(JsonNode element) {
            JsonNode from = element.path("from");
            JsonNode to = element.path("to");
            if (!from.isArray() || from.size() < 3 || !to.isArray() || to.size() < 3) {
                throw new IllegalArgumentException("Blockbench element must contain from/to arrays");
            }
            return new Box(
                    (float) from.get(0).asDouble() / BLOCKBENCH_UNIT,
                    (float) from.get(1).asDouble() / BLOCKBENCH_UNIT,
                    (float) from.get(2).asDouble() / BLOCKBENCH_UNIT,
                    (float) to.get(0).asDouble() / BLOCKBENCH_UNIT,
                    (float) to.get(1).asDouble() / BLOCKBENCH_UNIT,
                    (float) to.get(2).asDouble() / BLOCKBENCH_UNIT
            );
        }

        private float x(float corner) {
            return corner == 0.0f ? fromX : toX;
        }

        private float y(float corner) {
            return corner == 0.0f ? fromY : toY;
        }

        private float z(float corner) {
            return corner == 0.0f ? fromZ : toZ;
        }
    }

    private enum Face {
        EAST(new float[][] {{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}}),
        WEST(new float[][] {{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}}),
        UP(new float[][] {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}}),
        DOWN(new float[][] {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}}),
        SOUTH(new float[][] {{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}}),
        NORTH(new float[][] {{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}});

        private final float[][] corners;
        private final int[] indices = {0, 1, 2, 0, 2, 3};

        Face(float[][] corners) {
            this.corners = corners;
        }
    }
}
