package com.terralite.content.assets.model;

public record ContentModelVertex(float x, float y, float z, float u, float v, String textureSlot) {
    public ContentModelVertex(float x, float y, float z, float u, float v) {
        this(x, y, z, u, v, null);
    }
}
