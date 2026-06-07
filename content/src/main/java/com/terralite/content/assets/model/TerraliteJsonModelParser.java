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
            case "cross" -> cross(root);
            case "elements" -> elements(root.path("elements"));
            default -> throw new IllegalArgumentException(
                    "Unsupported Terralite JSON model type: " + type
                            + " (supported: cube_all, cube_column, cross, elements)"
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

    private static ContentModelMesh cross(JsonNode root) {
        float width = positiveFloat(root.path("width"), 1.0f, "cross width");
        float height = positiveFloat(root.path("height"), 1.0f, "cross height");
        float min = 0.5f - width / 2.0f;
        float max = 0.5f + width / 2.0f;

        List<ContentModelVertex> vertices = new ArrayList<>();
        addQuad(vertices, "all", new float[][] {
                {min, 0.0f, min}, {max, 0.0f, max}, {max, height, max}, {min, height, min}
        }, true);
        addQuad(vertices, "all", new float[][] {
                {max, 0.0f, min}, {min, 0.0f, max}, {min, height, max}, {max, height, min}
        }, true);
        return new ContentModelMesh(vertices);
    }

    private static ContentModelMesh elements(JsonNode elements) {
        if (!elements.isArray()) {
            throw new IllegalArgumentException("Terralite elements model must contain an elements array");
        }

        List<ContentModelVertex> vertices = new ArrayList<>();
        for (JsonNode element : elements) {
            Box box = Box.from(element);
            JsonNode faces = element.path("faces");
            for (Face face : Face.values()) {
                JsonNode faceNode = faces.path(face.name().toLowerCase());
                if (!faceNode.isMissingNode()) {
                    addFace(vertices, box, face, textureSlot(faceNode), uv(faceNode));
                }
            }
        }
        return new ContentModelMesh(vertices);
    }

    private static void addFace(List<ContentModelVertex> vertices, Face face, String textureSlot) {
        addQuad(vertices, textureSlot, face.corners, false);
    }

    private static void addFace(
            List<ContentModelVertex> vertices,
            Box box,
            Face face,
            String textureSlot,
            float[][] uvs
    ) {
        for (int index : face.indices) {
            float[] corner = face.corners[index];
            float[] uv = uvs[index];
            vertices.add(new ContentModelVertex(
                    box.x(corner[0]),
                    box.y(corner[1]),
                    box.z(corner[2]),
                    uv[0],
                    uv[1],
                    textureSlot
            ));
        }
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

    private static float[][] uv(JsonNode face) {
        JsonNode uv = face.path("uv");
        if (!uv.isArray() || uv.size() < 4) {
            return defaultUvs();
        }

        float u0 = (float) uv.get(0).asDouble();
        float v0 = (float) uv.get(1).asDouble();
        float u1 = (float) uv.get(2).asDouble();
        float v1 = (float) uv.get(3).asDouble();
        return new float[][] {
                {u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}
        };
    }

    private static float[][] defaultUvs() {
        return new float[][] {
                {0, 0}, {1, 0}, {1, 1}, {0, 1}
        };
    }

    private static String textureSlot(JsonNode face) {
        JsonNode texture = face.path("texture");
        if (!texture.isTextual() || texture.asText().isBlank()) {
            return null;
        }

        String slot = texture.asText();
        return slot.startsWith("#") ? slot.substring(1) : slot;
    }

    private static float positiveFloat(JsonNode node, float defaultValue, String label) {
        if (node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        float value = (float) node.asDouble();
        if (value <= 0.0f) {
            throw new IllegalArgumentException("Terralite " + label + " must be positive");
        }
        return value;
    }

    private record Box(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
        private static Box from(JsonNode element) {
            JsonNode from = element.path("from");
            JsonNode to = element.path("to");
            if (!from.isArray() || from.size() < 3 || !to.isArray() || to.size() < 3) {
                throw new IllegalArgumentException("Terralite element must contain from/to arrays");
            }
            return new Box(
                    (float) from.get(0).asDouble(),
                    (float) from.get(1).asDouble(),
                    (float) from.get(2).asDouble(),
                    (float) to.get(0).asDouble(),
                    (float) to.get(1).asDouble(),
                    (float) to.get(2).asDouble()
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
