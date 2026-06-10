package com.terralite.runtime.render;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A snapshot of per-bone rotations (in degrees) for a single animation frame.
 *
 * <p>Bone names match the {@code name} field of Blockbench elements, e.g.
 * {@code "Head"}, {@code "Arm_L"}, {@code "Leg_R"}.
 *
 * <p>Any bone not present in the map is treated as having zero rotation
 * (identity transform).
 */
public final class AnimationPose {

    /** Pose where all bones have zero rotation. */
    public static final AnimationPose IDENTITY = new AnimationPose(Map.of());

    private final Map<String, float[]> rotations; // bone name → [rotX, rotY, rotZ] in degrees

    public AnimationPose(Map<String, float[]> rotations) {
        Objects.requireNonNull(rotations, "rotations");
        this.rotations = Map.copyOf(rotations);
    }

    /**
     * Returns the rotation for the named bone as {@code [rotX, rotY, rotZ]} in degrees.
     * Returns {@code {0, 0, 0}} when the bone has no explicit entry in this pose.
     */
    public float[] rotationOf(String boneName) {
        float[] r = rotations.get(boneName);
        return r != null ? r : new float[]{0f, 0f, 0f};
    }

    /**
     * Returns a new pose identical to this one except the named bone's X rotation has
     * {@code deltaDegrees} added to it.  Used to inject camera pitch into the head bone
     * on top of whatever the current animation clip provides.
     */
    public AnimationPose withBoneAdditiveX(String boneName, float deltaDegrees) {
        if (deltaDegrees == 0f) return this;
        Map<String, float[]> merged = new HashMap<>(rotations);
        float[] existing = merged.getOrDefault(boneName, new float[]{0f, 0f, 0f});
        merged.put(boneName, new float[]{existing[0] + deltaDegrees, existing[1], existing[2]});
        return new AnimationPose(merged);
    }
}
