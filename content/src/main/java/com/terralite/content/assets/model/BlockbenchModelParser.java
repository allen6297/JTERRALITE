package com.terralite.content.assets.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Collections;

public final class BlockbenchModelParser {
    /** Blockbench position unit: 1/16 of a block. Always 16 regardless of texture resolution. */
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

        JsonNode root = mapper.readTree(reader);

        // Read texture resolution so UV pixel-coordinates can be normalized to 0-1.
        // Defaults to 16×16 (the Blockbench legacy unit) when not present.
        JsonNode resolution = root.path("resolution");
        float uvWidth  = resolution.path("width").isMissingNode()  ? BLOCKBENCH_UNIT : (float) resolution.path("width").asDouble();
        float uvHeight = resolution.path("height").isMissingNode() ? BLOCKBENCH_UNIT : (float) resolution.path("height").asDouble();

        JsonNode elements = root.path("elements");
        if (!elements.isArray()) {
            throw new IllegalArgumentException("Blockbench model must contain an elements array");
        }

        List<ContentModelBone> bones = new ArrayList<>();
        for (JsonNode element : elements) {
            String boneName = element.path("name").asText("bone");
            Box box = Box.from(element);
            float[] pivot = pivot(element);

            List<ContentModelVertex> boneVerts = new ArrayList<>();
            JsonNode faces = element.path("faces");
            for (Face face : Face.values()) {
                JsonNode faceNode = faces.path(face.name().toLowerCase());
                if (!faceNode.isMissingNode()) {
                    addFace(boneVerts, box, face, uv(faceNode, uvWidth, uvHeight));
                }
            }
            if (!boneVerts.isEmpty()) {
                bones.add(new ContentModelBone(boneName, pivot[0], pivot[1], pivot[2], boneVerts));
            }
        }

        return ContentModelMesh.ofBones(bones);
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

    /** Parses an element's {@code origin} array as a pivot point in block units (÷16). */
    private static float[] pivot(JsonNode element) {
        JsonNode origin = element.path("origin");
        if (!origin.isArray() || origin.size() < 3) {
            return new float[]{0f, 0f, 0f};
        }
        return new float[]{
                (float) origin.get(0).asDouble() / BLOCKBENCH_UNIT,
                (float) origin.get(1).asDouble() / BLOCKBENCH_UNIT,
                (float) origin.get(2).asDouble() / BLOCKBENCH_UNIT
        };
    }

    private static float[][] uv(JsonNode face, float uvWidth, float uvHeight) {
        JsonNode uv = face.path("uv");
        if (!uv.isArray() || uv.size() < 4) {
            return defaultUvs();
        }

        float u0 = (float) uv.get(0).asDouble() / uvWidth;
        float v0 = (float) uv.get(1).asDouble() / uvHeight;
        float u1 = (float) uv.get(2).asDouble() / uvWidth;
        float v1 = (float) uv.get(3).asDouble() / uvHeight;

        // Blockbench sometimes exports faces with swapped min/max to indicate mirroring.
        // Normalise so u0 ≤ u1 and v0 ≤ v1 (atlas remapping handles mirroring separately).
        if (u0 > u1) { float t = u0; u0 = u1; u1 = t; }
        if (v0 > v1) { float t = v0; v0 = v1; v1 = t; }

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
