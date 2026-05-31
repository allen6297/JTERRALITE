package com.terralite.content.assets.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ObjModelParser {
    public ContentModelMesh parse(Reader reader) throws IOException {
        Objects.requireNonNull(reader, "reader");

        List<Vec3> positions = new ArrayList<>();
        List<Vec2> uvs = new ArrayList<>();
        List<ContentModelVertex> vertices = new ArrayList<>();

        try (BufferedReader lines = new BufferedReader(reader)) {
            String line;
            int lineNumber = 0;
            while ((line = lines.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\\s+");
                switch (parts[0]) {
                    case "v" -> positions.add(parsePosition(parts, lineNumber));
                    case "vt" -> uvs.add(parseUv(parts, lineNumber));
                    case "f" -> addFace(vertices, positions, uvs, parts, lineNumber);
                    default -> {
                    }
                }
            }
        }

        return new ContentModelMesh(vertices);
    }

    private static Vec3 parsePosition(String[] parts, int lineNumber) {
        if (parts.length < 4) {
            throw new IllegalArgumentException("OBJ line " + lineNumber + " position requires x y z");
        }
        return new Vec3(parseFloat(parts[1], lineNumber), parseFloat(parts[2], lineNumber), parseFloat(parts[3], lineNumber));
    }

    private static Vec2 parseUv(String[] parts, int lineNumber) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("OBJ line " + lineNumber + " texture coordinate requires u v");
        }
        return new Vec2(parseFloat(parts[1], lineNumber), parseFloat(parts[2], lineNumber));
    }

    private static void addFace(
            List<ContentModelVertex> vertices,
            List<Vec3> positions,
            List<Vec2> uvs,
            String[] parts,
            int lineNumber
    ) {
        if (parts.length != 4) {
            throw new IllegalArgumentException("OBJ line " + lineNumber + " only triangular faces are supported");
        }
        for (int i = 1; i < parts.length; i++) {
            vertices.add(parseFaceVertex(parts[i], positions, uvs, lineNumber));
        }
    }

    private static ContentModelVertex parseFaceVertex(
            String value,
            List<Vec3> positions,
            List<Vec2> uvs,
            int lineNumber
    ) {
        String[] parts = value.split("/", -1);
        int positionIndex = parseIndex(parts[0], positions.size(), lineNumber, "position");
        Vec3 position = positions.get(positionIndex);

        Vec2 uv = new Vec2(0.0f, 0.0f);
        if (parts.length > 1 && !parts[1].isBlank()) {
            int uvIndex = parseIndex(parts[1], uvs.size(), lineNumber, "texture coordinate");
            uv = uvs.get(uvIndex);
        }

        return new ContentModelVertex(position.x(), position.y(), position.z(), uv.u(), uv.v());
    }

    private static int parseIndex(String value, int size, int lineNumber, String label) {
        int index;
        try {
            index = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("OBJ line " + lineNumber + " has invalid " + label + " index: " + value, exception);
        }

        int resolved = index > 0 ? index - 1 : size + index;
        if (resolved < 0 || resolved >= size) {
            throw new IllegalArgumentException("OBJ line " + lineNumber + " references missing " + label + " index: " + value);
        }
        return resolved;
    }

    private static float parseFloat(String value, int lineNumber) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("OBJ line " + lineNumber + " has invalid number: " + value, exception);
        }
    }

    private record Vec2(float u, float v) {
    }

    private record Vec3(float x, float y, float z) {
    }
}
