package com.terralite.render.texture;

public record TextureRegion(float u0, float v0, float u1, float v1) {
    public TextureRegion {
        if (u0 < 0.0f || v0 < 0.0f || u1 > 1.0f || v1 > 1.0f || u1 < u0 || v1 < v0) {
            throw new IllegalArgumentException("Invalid texture region");
        }
    }

    public float mapU(float u) {
        return u0 + (u1 - u0) * u;
    }

    public float mapV(float v) {
        return v0 + (v1 - v0) * v;
    }
}
