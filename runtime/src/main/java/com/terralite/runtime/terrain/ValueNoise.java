package com.terralite.runtime.terrain;

/**
 * Seeded 2D value noise with smooth quintic interpolation.
 * Use {@link #fbm} for fractal (multi-octave) terrain heights.
 */
public final class ValueNoise {
    private final long seed;

    public ValueNoise(long seed) {
        this.seed = seed;
    }

    /** Fractal Brownian Motion — sums {@code octaves} noise layers. Returns value in [0, 1]. */
    public double fbm(double x, double z, int octaves, double persistence, double lacunarity) {
        double value = 0, amplitude = 1, frequency = 1, total = 0;
        for (int i = 0; i < octaves; i++) {
            value += noise(x * frequency, z * frequency) * amplitude;
            total += amplitude;
            amplitude  *= persistence;
            frequency  *= lacunarity;
        }
        return value / total;
    }

    /** Single-octave value noise, returns [0, 1]. */
    public double noise(double x, double z) {
        int ix = fastFloor(x);
        int iz = fastFloor(z);
        double fx = x - ix;
        double fz = z - iz;

        // Quintic smoothstep
        double ux = quintic(fx);
        double uz = quintic(fz);

        double a = hash(ix,     iz);
        double b = hash(ix + 1, iz);
        double c = hash(ix,     iz + 1);
        double d = hash(ix + 1, iz + 1);

        return lerp(lerp(a, b, ux), lerp(c, d, ux), uz);
    }

    private double hash(int x, int z) {
        long n = seed ^ ((long) x * 1619L) ^ ((long) z * 31337L);
        n = (n ^ (n >>> 30)) * 0xbf58476d1ce4e5b9L;
        n = (n ^ (n >>> 27)) * 0x94d049bb133111ebL;
        n ^= (n >>> 31);
        return (double) (n & 0x7fffffffffffffffL) / (double) 0x7fffffffffffffffL;
    }

    private static double quintic(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
}
