package com.terralite.runtime.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Generates a default 64×64 player skin texture at runtime.
 *
 * <p>Layout follows the standard Minecraft Java Edition skin format:
 * <pre>
 *   Row 0 (y 0–15):  head sides + top/bottom
 *   Row 1 (y 16–31): right leg, body, right arm
 *   Row 2 (y 32–47): (overlay layer — left blank)
 *   Row 3 (y 48–63): left leg, left arm
 * </pre>
 *
 * <p>UV constants are expressed in 0–1 normalized space for use by
 * the hardcoded fallback geometry in {@link PlayerBoxBuilder}.
 */
public final class PlayerSkinGenerator {
    // UV constants for fallback geometry (normalized 0-1 over the 64×64 skin)
    // Head front face: pixels (8,8)-(16,16)
    public static final float HEAD_U0 = 8f/64,  HEAD_V0 = 8f/64,  HEAD_U1 = 16f/64, HEAD_V1 = 16f/64;
    // Body front face: pixels (20,20)-(28,32)
    public static final float BODY_U0 = 20f/64, BODY_V0 = 20f/64, BODY_U1 = 28f/64, BODY_V1 = 32f/64;
    // Right arm front: pixels (44,20)-(48,32)
    public static final float ARM_U0  = 44f/64, ARM_V0  = 20f/64, ARM_U1  = 48f/64, ARM_V1  = 32f/64;
    // Right leg front: pixels (4,20)-(8,32)
    public static final float LEG_U0  = 4f/64,  LEG_V0  = 20f/64, LEG_U1  = 8f/64,  LEG_V1  = 32f/64;

    private static final Color SKIN  = new Color(0xF4C58D);
    private static final Color SHIRT = new Color(0x5B8DD9);
    private static final Color PANTS = new Color(0x2C3E6B);
    private static final Color HAIR  = new Color(0x5C3D1E);
    private static final Color EYE   = new Color(0x1A1A2E);
    private static final Color WHITE = new Color(0xFFFFFF);
    private static final Color SHOE  = new Color(0x1A1008);

    private PlayerSkinGenerator() {}

    /** Returns a 64×64 ARGB {@link BufferedImage} in standard Minecraft skin layout. */
    public static BufferedImage generate() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            drawHead(g);
            drawBody(g);
            drawRightArm(g);
            drawLeftArm(g);
            drawRightLeg(g);
            drawLeftLeg(g);
        } finally {
            g.dispose();
        }
        return img;
    }

    // Head — (0,0)-(32,16): four sides at y=8-16, caps at x=8-24 y=0-8
    private static void drawHead(Graphics2D g) {
        g.setColor(SKIN);
        g.fillRect(0, 8, 32, 8);
        g.fillRect(8, 0, 16, 8);
        // Front face hair band and eyes
        g.setColor(HAIR);
        g.fillRect(8, 8, 8, 3);
        g.setColor(WHITE);
        g.fillRect(9, 12, 2, 2);
        g.fillRect(13, 12, 2, 2);
        g.setColor(EYE);
        g.fillRect(10, 12, 1, 1);
        g.fillRect(14, 12, 1, 1);
    }

    // Body — (16,16)-(40,32)
    private static void drawBody(Graphics2D g) {
        g.setColor(SHIRT);
        g.fillRect(16, 16, 24, 16);
        g.setColor(SKIN);
        g.fillRect(20, 20, 8, 1);  // collar strip
    }

    // Right arm — (40,16)-(56,32)
    private static void drawRightArm(Graphics2D g) {
        g.setColor(SHIRT);
        g.fillRect(40, 16, 16, 16);
        g.setColor(SKIN);
        g.fillRect(44, 28, 4, 4);  // wrist cuff
    }

    // Left arm — (32,48)-(48,64)
    private static void drawLeftArm(Graphics2D g) {
        g.setColor(SHIRT);
        g.fillRect(32, 48, 16, 16);
        g.setColor(SKIN);
        g.fillRect(36, 60, 4, 4);
    }

    // Right leg — (0,16)-(16,32)
    private static void drawRightLeg(Graphics2D g) {
        g.setColor(PANTS);
        g.fillRect(0, 16, 16, 16);
        g.setColor(SHOE);
        g.fillRect(4, 28, 4, 4);
    }

    // Left leg — (16,48)-(32,64)
    private static void drawLeftLeg(Graphics2D g) {
        g.setColor(PANTS);
        g.fillRect(16, 48, 16, 16);
        g.setColor(SHOE);
        g.fillRect(20, 60, 4, 4);
    }
}
