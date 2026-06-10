package com.terralite.runtime.render;

import com.terralite.content.assets.model.ContentModelBone;
import com.terralite.content.assets.model.ContentModelMesh;
import com.terralite.content.assets.model.ContentModelVertex;
import com.terralite.core.registry.ResourceId;
import com.terralite.render.RenderChunk;
import com.terralite.render.RenderChunkMesh;
import com.terralite.render.mesh.DebugMesh;
import com.terralite.render.mesh.DebugVertex;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a textured humanoid mesh for a player entity.
 *
 * <p>When a {@link ContentModelMesh} is supplied the geometry comes from that model
 * (typically loaded from {@code packs/terralite/assets/models/entity/player.bbmodel}).
 * If no model is available the builder falls back to hardcoded box geometry so the
 * game never crashes because of a missing asset.
 *
 * <p>Either way every vertex references {@link #SKIN_ID} so the skin texture loaded
 * into the atlas ({@code packs/terralite/assets/textures/entity/player.png}) is
 * applied automatically after UV remapping.
 */
public final class PlayerBoxBuilder {
    public static final ResourceId SKIN_ID = ResourceId.id("terralite:entity/player");

    /** ResourceId used to look up the player model in the content model library. */
    public static final ResourceId MODEL_ID = ResourceId.id("terralite:entity/player");

    // Fallback body-part dimensions (y = feet)
    private static final float HW  = 0.25f; // head half-width
    private static final float HH  = 0.50f; // head height
    private static final float BHW = 0.20f; // torso half-width (X)
    private static final float BHD = 0.12f; // torso half-depth (Z)
    private static final float BH  = 0.60f; // torso height
    private static final float LW  = 0.09f; // limb half-width
    private static final float LH  = 0.60f; // limb height

    private PlayerBoxBuilder() {}

    /**
     * Builds a player mesh from a pack-provided model with full animation and head-look support.
     *
     * @param playerId     unique identifier used as the render chunk key
     * @param x            feet X position in world space
     * @param y            feet Y position in world space
     * @param z            feet Z position in world space
     * @param yawDegrees   camera yaw in degrees; the whole body rotates to face this direction
     * @param pitchDegrees camera pitch in degrees; only the head bone rotates vertically
     * @param model        mesh loaded from the content pack, or {@code null} to use fallback geometry
     * @param pose         bone rotations for this frame, or {@link AnimationPose#IDENTITY} for no animation
     */
    public static RenderChunkMesh build(String playerId, double x, double y, double z,
                                        double yawDegrees, double pitchDegrees,
                                        ContentModelMesh model, AnimationPose pose) {
        float fx = (float) x;
        float fy = (float) y;
        float fz = (float) z;
        float cosY = (float) Math.cos(Math.toRadians(yawDegrees));
        float sinY = (float) Math.sin(Math.toRadians(yawDegrees));
        AnimationPose p = (pose != null ? pose : AnimationPose.IDENTITY)
                .withBoneAdditiveX("Head", (float) pitchDegrees);

        List<DebugVertex> verts = (model != null && model.hasBonesData())
                ? fromBones(model, fx, fy, fz, cosY, sinY, p)
                : (model != null)
                    ? fromModel(model, fx, fy, fz, cosY, sinY)
                    : fromFallback(fx, fy, fz, cosY, sinY);

        RenderChunk chunk = new RenderChunk(Integer.MIN_VALUE + 1, playerId.hashCode(), 0);
        return new RenderChunkMesh(chunk, new DebugMesh(verts));
    }

    /** Convenience overload — no pitch, no animation. */
    public static RenderChunkMesh build(String playerId, double x, double y, double z,
                                        double yawDegrees, ContentModelMesh model, AnimationPose pose) {
        return build(playerId, x, y, z, yawDegrees, 0.0, model, pose);
    }

    /** Convenience overload — no yaw, no pitch, no animation. */
    public static RenderChunkMesh build(String playerId, double x, double y, double z,
                                        double yawDegrees, ContentModelMesh model) {
        return build(playerId, x, y, z, yawDegrees, 0.0, model, AnimationPose.IDENTITY);
    }

    /** Convenience overload — no yaw, no pitch, no animation (used internally and in tests). */
    public static RenderChunkMesh build(String playerId, double x, double y, double z) {
        return build(playerId, x, y, z, 0.0, 0.0, null, AnimationPose.IDENTITY);
    }

    // -------------------------------------------------------------------------
    // Model-driven path
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Animated path — uses per-bone data from the bbmodel
    // -------------------------------------------------------------------------

    /**
     * Iterates each bone, applies its rotation from {@code pose} around its pivot, then
     * applies the world yaw rotation and translates to world space.
     *
     * <p>Rotation order per bone: Y (local heading) → X (pitch) → Z (roll) → world Y (body yaw).
     * The head bone receives camera pitch via {@code pose} so it nods up and down independently
     * of the body.
     */
    private static List<DebugVertex> fromBones(ContentModelMesh model,
                                               float fx, float fy, float fz,
                                               float cosY, float sinY,
                                               AnimationPose pose) {
        List<DebugVertex> out = new ArrayList<>();
        for (ContentModelBone bone : model.bones()) {
            float[] rot = pose.rotationOf(bone.name());
            double ry = Math.toRadians(rot[1]);
            double rx = Math.toRadians(rot[0]);
            double rz = Math.toRadians(rot[2]);
            float cBY = (float) Math.cos(ry), sBY = (float) Math.sin(ry);
            float cX  = (float) Math.cos(rx), sX  = (float) Math.sin(rx);
            float cZ  = (float) Math.cos(rz), sZ  = (float) Math.sin(rz);

            float px = bone.pivotX();
            float py = bone.pivotY();
            float pz = bone.pivotZ();

            for (ContentModelVertex v : bone.vertices()) {
                // Translate to pivot space
                float lx = v.x() - px;
                float ly = v.y() - py;
                float lz = v.z() - pz;

                // Rotate around Y axis (bone-local heading — head left/right look)
                float lx1 =  lx * cBY + lz * sBY;
                float lz1 = -lx * sBY + lz * cBY;

                // Rotate around X axis (pitch — arm/leg swing + head nod)
                float ly2 = ly  * cX - lz1 * sX;
                float lz2 = ly  * sX + lz1 * cX;

                // Rotate around Z axis (roll — side tilt)
                float lx3 = lx1 * cZ - ly2 * sZ;
                float ly3 = lx1 * sZ + ly2 * cZ;
                float lz3 = lz2;

                // Translate back from pivot
                float wx = lx3 + px;
                float wy = ly3 + py;
                float wz = lz3 + pz;

                // Apply world (body) yaw and world translation
                out.add(new DebugVertex(
                        fx + wx * cosY + wz * sinY,
                        fy + wy,
                        fz + (-wx * sinY + wz * cosY),
                        1f, 1f, 1f, 1f,
                        v.u(), v.v(),
                        SKIN_ID));
            }
        }
        return out;
    }

    /**
     * Converts pack model vertices to {@link DebugVertex} values, rotated by yaw and
     * offset to the player's world position.
     *
     * <p>The bbmodel coordinate system has Y=0 at the player's feet and uses Blockbench
     * units (1 unit = 1/16 block). The {@link com.terralite.content.assets.model.BlockbenchModelParser}
     * already divides all coordinates by 16, so the vertices arrive here in block units.
     * A Y-axis rotation is applied around the origin before translating to world space.
     */
    private static List<DebugVertex> fromModel(ContentModelMesh model,
                                               float fx, float fy, float fz,
                                               float cosY, float sinY) {
        List<ContentModelVertex> src = model.vertices();
        List<DebugVertex> out = new ArrayList<>(src.size());
        for (ContentModelVertex v : src) {
            float rx = v.x() * cosY + v.z() * sinY;
            float rz = -v.x() * sinY + v.z() * cosY;
            out.add(new DebugVertex(
                    fx + rx, fy + v.y(), fz + rz,
                    1f, 1f, 1f, 1f,
                    v.u(), v.v(),
                    SKIN_ID));
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Hardcoded fallback geometry
    // -------------------------------------------------------------------------

    private static List<DebugVertex> fromFallback(float fx, float fy, float fz,
                                                   float cosY, float sinY) {
        List<DebugVertex> verts = new ArrayList<>();

        // All coordinates are in model space (Y=0 at feet, X/Z centred at 0).
        // They are rotated by (cosY, sinY) and then translated to world space.

        // Head: y [1.2, 1.7]
        box(verts, -HW, 1.20f, -HW, HW, 1.20f + HH, HW,
                PlayerSkinGenerator.HEAD_U0, PlayerSkinGenerator.HEAD_V0,
                PlayerSkinGenerator.HEAD_U1, PlayerSkinGenerator.HEAD_V1,
                fx, fy, fz, cosY, sinY);

        // Torso: y [0.6, 1.2]
        box(verts, -BHW, 0.60f, -BHD, BHW, 0.60f + BH, BHD,
                PlayerSkinGenerator.BODY_U0, PlayerSkinGenerator.BODY_V0,
                PlayerSkinGenerator.BODY_U1, PlayerSkinGenerator.BODY_V1,
                fx, fy, fz, cosY, sinY);

        // Left arm
        box(verts, -BHW - LW * 3, 0.60f, -LW, -BHW - LW, 0.60f + LH, LW,
                PlayerSkinGenerator.ARM_U0, PlayerSkinGenerator.ARM_V0,
                PlayerSkinGenerator.ARM_U1, PlayerSkinGenerator.ARM_V1,
                fx, fy, fz, cosY, sinY);

        // Right arm
        box(verts, BHW + LW, 0.60f, -LW, BHW + LW * 3, 0.60f + LH, LW,
                PlayerSkinGenerator.ARM_U0, PlayerSkinGenerator.ARM_V0,
                PlayerSkinGenerator.ARM_U1, PlayerSkinGenerator.ARM_V1,
                fx, fy, fz, cosY, sinY);

        // Left leg: y [0, 0.6]
        box(verts, -LW * 2, 0f, -LW, 0f, LH, LW,
                PlayerSkinGenerator.LEG_U0, PlayerSkinGenerator.LEG_V0,
                PlayerSkinGenerator.LEG_U1, PlayerSkinGenerator.LEG_V1,
                fx, fy, fz, cosY, sinY);

        // Right leg
        box(verts, 0f, 0f, -LW, LW * 2, LH, LW,
                PlayerSkinGenerator.LEG_U0, PlayerSkinGenerator.LEG_V0,
                PlayerSkinGenerator.LEG_U1, PlayerSkinGenerator.LEG_V1,
                fx, fy, fz, cosY, sinY);

        return verts;
    }

    /** Emits a box in model space, rotating each corner by (cosY, sinY) before world translation. */
    private static void box(List<DebugVertex> v,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float u0, float vv0, float u1, float vv1,
                             float fx, float fy, float fz, float cosY, float sinY) {
        // Eight corners (model space)
        float[][] c = {
            {x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1},
            {x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}
        };
        // bottom (0,1,2,3), top (7,6,5,4), north (0,4,5,1), south (2,6,7,3), west (3,7,4,0), east (1,5,6,2)
        int[][] faces = {{0,1,2,3},{7,6,5,4},{0,4,5,1},{2,6,7,3},{3,7,4,0},{1,5,6,2}};
        for (int[] fi : faces) {
            v.add(rv(c[fi[0]], u0,vv0, fx,fy,fz,cosY,sinY));
            v.add(rv(c[fi[1]], u1,vv0, fx,fy,fz,cosY,sinY));
            v.add(rv(c[fi[2]], u1,vv1, fx,fy,fz,cosY,sinY));
            v.add(rv(c[fi[0]], u0,vv0, fx,fy,fz,cosY,sinY));
            v.add(rv(c[fi[2]], u1,vv1, fx,fy,fz,cosY,sinY));
            v.add(rv(c[fi[3]], u0,vv1, fx,fy,fz,cosY,sinY));
        }
    }

    private static DebugVertex rv(float[] p, float u, float vv,
                                   float fx, float fy, float fz, float cosY, float sinY) {
        float rx = p[0] * cosY + p[2] * sinY;
        float rz = -p[0] * sinY + p[2] * cosY;
        return new DebugVertex(fx + rx, fy + p[1], fz + rz, 1f, 1f, 1f, 1f, u, vv, SKIN_ID);
    }
}
