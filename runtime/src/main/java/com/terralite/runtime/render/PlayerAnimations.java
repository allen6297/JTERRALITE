package com.terralite.runtime.render;

import java.util.Map;

/**
 * Built-in animation clips for the player model.
 *
 * <p>All methods are pure functions of time — they produce an {@link AnimationPose}
 * for the given elapsed seconds and can be blended or switched freely.
 *
 * <p>Bone names correspond to the element names in
 * {@code packs/terralite/assets/models/entity/player.bbmodel}.
 */
public final class PlayerAnimations {

    /** Limb swing amplitude for the walk cycle (degrees). */
    private static final float WALK_AMPLITUDE = 30.0f;

    /** Walk cycle frequency (full cycles per second — one cycle = two steps). */
    private static final double WALK_FREQUENCY = 1.8;

    /** Idle breathing bob amplitude (degrees). */
    private static final float IDLE_AMPLITUDE = 1.5f;

    /** Idle breathing frequency (cycles per second). */
    private static final double IDLE_FREQUENCY = 0.4;

    private PlayerAnimations() {}

    /**
     * Walk-cycle pose.
     *
     * @param walkSeconds cumulative seconds the player has been walking;
     *                    advancing this value drives the limb swing
     */
    public static AnimationPose walk(double walkSeconds) {
        float swing = (float) (Math.sin(walkSeconds * WALK_FREQUENCY * Math.PI * 2) * WALK_AMPLITUDE);
        return new AnimationPose(Map.of(
                "Arm_L", new float[]{ swing,  0f, 0f},
                "Arm_R", new float[]{-swing,  0f, 0f},
                "Leg_L", new float[]{-swing,  0f, 0f},
                "Leg_R", new float[]{ swing,  0f, 0f}
        ));
    }

    /**
     * Idle pose — subtle breathing head movement.
     *
     * @param totalSeconds wall-clock seconds since the game started (drives the bob)
     */
    public static AnimationPose idle(double totalSeconds) {
        float bob = (float) (Math.sin(totalSeconds * IDLE_FREQUENCY * Math.PI * 2) * IDLE_AMPLITUDE);
        return new AnimationPose(Map.of(
                "Head", new float[]{bob, 0f, 0f}
        ));
    }
}
