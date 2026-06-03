package com.terralite.engine.terrain;

import com.terralite.engine.world.World;

import java.util.Optional;

public final class BlockRaycaster {
    private BlockRaycaster() {
    }

    public record HitResult(BlockPos blockPos, BlockPos adjacentPos) {
        /** Unit normal pointing from the hit block toward the adjacent (air) position. */
        public int normalX() { return adjacentPos.x() - blockPos.x(); }
        public int normalY() { return adjacentPos.y() - blockPos.y(); }
        public int normalZ() { return adjacentPos.z() - blockPos.z(); }
    }

    /**
     * DDA ray-march through world block storage. Returns the first occupied block
     * within {@code maxDistance}, plus the adjacent air position (for placement).
     */
    public static Optional<HitResult> cast(
            World world,
            double originX, double originY, double originZ,
            double dirX, double dirY, double dirZ,
            double maxDistance
    ) {
        double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len < 1e-9) {
            return Optional.empty();
        }
        dirX /= len;
        dirY /= len;
        dirZ /= len;

        int bx = (int) Math.floor(originX);
        int by = (int) Math.floor(originY);
        int bz = (int) Math.floor(originZ);

        int stepX = dirX > 0 ? 1 : -1;
        int stepY = dirY > 0 ? 1 : -1;
        int stepZ = dirZ > 0 ? 1 : -1;

        double tDeltaX = Math.abs(dirX) < 1e-9 ? Double.MAX_VALUE : Math.abs(1.0 / dirX);
        double tDeltaY = Math.abs(dirY) < 1e-9 ? Double.MAX_VALUE : Math.abs(1.0 / dirY);
        double tDeltaZ = Math.abs(dirZ) < 1e-9 ? Double.MAX_VALUE : Math.abs(1.0 / dirZ);

        double tMaxX = tDeltaX * (stepX > 0 ? (bx + 1 - originX) : (originX - bx));
        double tMaxY = tDeltaY * (stepY > 0 ? (by + 1 - originY) : (originY - by));
        double tMaxZ = tDeltaZ * (stepZ > 0 ? (bz + 1 - originZ) : (originZ - bz));

        int prevX = bx, prevY = by, prevZ = bz;

        while (true) {
            if (world.blocks().contains(BlockPos.of(bx, by, bz))) {
                return Optional.of(new HitResult(
                        BlockPos.of(bx, by, bz),
                        BlockPos.of(prevX, prevY, prevZ)
                ));
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (tMaxX > maxDistance) break;
                    prevX = bx; prevY = by; prevZ = bz;
                    bx += stepX;
                    tMaxX += tDeltaX;
                } else {
                    if (tMaxZ > maxDistance) break;
                    prevX = bx; prevY = by; prevZ = bz;
                    bz += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (tMaxY > maxDistance) break;
                    prevX = bx; prevY = by; prevZ = bz;
                    by += stepY;
                    tMaxY += tDeltaY;
                } else {
                    if (tMaxZ > maxDistance) break;
                    prevX = bx; prevY = by; prevZ = bz;
                    bz += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }
        return Optional.empty();
    }
}
