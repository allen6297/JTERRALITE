package com.terralite.content.assets.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TerraliteJsonModelParser {
    private final ObjectMapper mapper;

    public TerraliteJsonModelParser() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public TerraliteJsonModelParser(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public ContentModelMesh parse(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "reader");

        JsonNode root = mapper.readTree(reader);
        JsonNode typeNode = root.path("type");
        if (!typeNode.isTextual() || typeNode.asText().isBlank()) {
            throw new IllegalArgumentException("Terralite JSON model must define a non-blank string type");
        }
        String type = typeNode.asText();
        return switch (type) {
            case "cube_all" -> cubeAll();
            case "cube_column" -> cubeColumn();
            case "cross" -> cross();
            default -> throw new IllegalArgumentException(
                    "Unsupported Terralite JSON model type: " + type
                            + " (supported: cube_all, cube_column, cross)"
            );
        };
    }

    private static ContentModelMesh cubeAll() {
        List<ContentModelVertex> vertices = new ArrayList<>();
        for (Face face : Face.values()) {
            addFace(vertices, face, "all");
        }
        return new ContentModelMesh(vertices);
    }

    private static ContentModelMesh cubeColumn() {
        List<ContentModelVertex> vertices = new ArrayList<>();
        for (Face face : Face.values()) {
            addFace(vertices, face, switch (face) {
                case UP -> "top";
                case DOWN -> "bottom";
                case EAST, WEST, SOUTH, NORTH -> "side";
            });
        }
        return new ContentModelMesh(vertices);
    }

    private static ContentModelMesh cross() {
        List<ContentModelVertex> vertices = new ArrayList<>();
        addQuad(vertices, "all", new float[][] {
                {0.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 1.0f}, {1.0f, 1.0f, 1.0f}, {0.0f, 1.0f, 0.0f}
        }, true);
        addQuad(vertices, "all", new float[][] {
                {1.0f, 0.0f, 0.0f}, {0.0f, 0.0f, 1.0f}, {0.0f, 1.0f, 1.0f}, {1.0f, 1.0f, 0.0f}
        }, true);
        return new ContentModelMesh(vertices);
    }

    private static void addFace(List<ContentModelVertex> vertices, Face face, String textureSlot) {
        addQuad(vertices, textureSlot, face.corners, false);
    }

    private static void addQuad(
            List<ContentModelVertex> vertices,
            String textureSlot,
            float[][] corners,
            boolean doubleSided
    ) {
        addTriangle(vertices, textureSlot, corners, 0, 1, 2);
        addTriangle(vertices, textureSlot, corners, 0, 2, 3);
        if (doubleSided) {
            addTriangle(vertices, textureSlot, corners, 2, 1, 0);
            addTriangle(vertices, textureSlot, corners, 3, 2, 0);
        }
    }

    private static void addTriangle(
            List<ContentModelVertex> vertices,
            String textureSlot,
            float[][] corners,
            int first,
            int second,
            int third
    ) {
        addVertex(vertices, corners[first], uv(first), textureSlot);
        addVertex(vertices, corners[second], uv(second), textureSlot);
        addVertex(vertices, corners[third], uv(third), textureSlot);
    }

    private static void addVertex(List<ContentModelVertex> vertices, float[] corner, float[] uv, String textureSlot) {
        vertices.add(new ContentModelVertex(corner[0], corner[1], corner[2], uv[0], uv[1], textureSlot));
    }

    private static float[] uv(int cornerIndex) {
        return switch (cornerIndex) {
            case 0 -> new float[] {0.0f, 0.0f};
            case 1 -> new float[] {1.0f, 0.0f};
            case 2 -> new float[] {1.0f, 1.0f};
            case 3 -> new float[] {0.0f, 1.0f};
            default -> throw new IllegalArgumentException("Invalid quad corner: " + cornerIndex);
        };
    }

    private enum Face {
        EAST(new float[][] {{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}}),
        WEST(new float[][] {{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}}),
        UP(new float[][] {{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}}),
        DOWN(new float[][] {{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}}),
        SOUTH(new float[][] {{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}}),
        NORTH(new float[][] {{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}});

        private final float[][] corners;

        Face(float[][] corners) {
            this.corners = corners;
        }
    }
}
