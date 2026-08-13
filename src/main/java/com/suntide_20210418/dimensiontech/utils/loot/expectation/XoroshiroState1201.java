package com.suntide_20210418.dimensiontech.utils.loot.expectation;

/** Immutable Minecraft 1.20.1 Xoroshiro128++ state and exact deterministic transitions. */
public record XoroshiroState1201(long seedLo, long seedHi) {
    private static final long SILVER_RATIO_64 = -7046029254386353131L;
    private static final long GOLDEN_RATIO_64 = 7640891576956012809L;

    public XoroshiroState1201 {
        if ((seedLo | seedHi) == 0L) {
            seedLo = SILVER_RATIO_64;
            seedHi = GOLDEN_RATIO_64;
        }
    }

    public Draw<Long> nextLong() {
        long low = seedLo;
        long high = seedHi;
        long value = Long.rotateLeft(low + high, 17) + low;
        high ^= low;
        long nextLow = Long.rotateLeft(low, 49) ^ high ^ (high << 21);
        long nextHigh = Long.rotateLeft(high, 28);
        return new Draw<>(value, new XoroshiroState1201(nextLow, nextHigh), 1);
    }

    public Draw<Integer> nextInt() {
        Draw<Long> draw = nextLong();
        return new Draw<>((int) (long) draw.value(), draw.state(), draw.drawCount());
    }

    public Draw<Integer> nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("Bound must be positive");
        XoroshiroState1201 state = this;
        int draws = 0;
        long product;
        long lowBits;
        long threshold = Integer.toUnsignedLong(
                Integer.remainderUnsigned(~bound + 1, bound));
        do {
            Draw<Integer> draw = state.nextInt();
            state = draw.state();
            draws += draw.drawCount();
            product = Integer.toUnsignedLong(draw.value()) * (long) bound;
            lowBits = product & 0xffffffffL;
        } while (lowBits < Integer.toUnsignedLong(bound) && lowBits < threshold);
        return new Draw<>((int) (product >> 32), state, draws);
    }

    public Draw<Boolean> nextBoolean() {
        Draw<Long> draw = nextLong();
        return new Draw<>((draw.value() & 1L) != 0L, draw.state(), draw.drawCount());
    }

    public Draw<Float> nextFloat() {
        Draw<Long> draw = nextLong();
        float value = (float) (draw.value() >>> 40) * 0x1.0p-24F;
        return new Draw<>(value, draw.state(), draw.drawCount());
    }

    public Draw<Double> nextDouble() {
        Draw<Long> draw = nextLong();
        double value = (double) (draw.value() >>> 11) * 0x1.0p-53D;
        return new Draw<>(value, draw.state(), draw.drawCount());
    }

    public Draw<Integer> nextIntInclusive(int min, int max) {
        long size = (long) max - min + 1L;
        if (size <= 0L || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid inclusive range");
        }
        Draw<Integer> draw = nextInt((int) size);
        return new Draw<>(min + draw.value(), draw.state(), draw.drawCount());
    }

    public record Draw<T>(T value, XoroshiroState1201 state, int drawCount) {}
}
