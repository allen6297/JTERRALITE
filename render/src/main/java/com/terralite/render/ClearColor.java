package com.terralite.render;

public record ClearColor(float red, float green, float blue, float alpha) {
    public static final ClearColor BLACK = new ClearColor(0.0f, 0.0f, 0.0f, 1.0f);

    public ClearColor {
        validate(red, "red");
        validate(green, "green");
        validate(blue, "blue");
        validate(alpha, "alpha");
    }

    private static void validate(float value, String channel) {
        if (value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException("Clear color " + channel + " channel must be between 0 and 1");
        }
    }
}
