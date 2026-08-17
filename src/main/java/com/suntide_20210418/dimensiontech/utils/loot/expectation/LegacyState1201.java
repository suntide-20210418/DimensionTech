package com.suntide_20210418.dimensiontech.utils.loot.expectation;

/**
 * Immutable Minecraft 1.20.1 {@code LegacyRandomSource} state.
 *
 * <p>{@code LegacyRandomSource} is the Java-compatible 48-bit linear congruential generator. The
 * value stored by this class is the generator's internal (already scrambled) 48-bit seed, rather
 * than the user-facing seed passed to the constructor. Keeping that distinction explicit is
 * important when a continuation is persisted or used as a key in a finite state distribution.
 */
public record LegacyState1201(long internalSeed) {
    private static final long MULTIPLIER = 25214903917L;
    private static final long INCREMENT = 11L;
    private static final long MODULUS_MASK = (1L << 48) - 1L;

    public LegacyState1201 {
        // The runtime stores only the low 48 bits after every transition.
        internalSeed &= MODULUS_MASK;
    }

    /** Creates the exact internal state produced by {@code new LegacyRandomSource(seed)}. */
    public static LegacyState1201 fromSeed(long seed) {
        return new LegacyState1201((seed ^ MULTIPLIER) & MODULUS_MASK);
    }

    /** Executes the source's protected {@code next(bits)} primitive. */
    public Draw<Integer> nextBits(int bits) {
        if (bits < 1 || bits > 32) {
            throw new IllegalArgumentException("bits must be in [1, 32]");
        }
        long next = (internalSeed * MULTIPLIER + INCREMENT) & MODULUS_MASK;
        return new Draw<>((int) (next >>> (48 - bits)), new LegacyState1201(next), 1);
    }

    /** Equivalent to {@link net.minecraft.util.RandomSource#nextInt()}. */
    public Draw<Integer> nextInt() {
        return nextBits(32);
    }

    /**
     * Exact {@code BitRandomSource.nextInt(int)} semantics, including its power-of-two fast path
     * and signed rejection condition.
     */
    public Draw<Integer> nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("Bound must be positive");
        LegacyState1201 state = this;
        int draws = 0;
        if ((bound & (bound - 1)) == 0) {
            Draw<Integer> bits = state.nextBits(31);
            return new Draw<>((int) ((bound * (long) bits.value()) >> 31), bits.state(), 1);
        }
        int bits;
        int value;
        do {
            Draw<Integer> draw = state.nextBits(31);
            state = draw.state();
            draws += draw.drawCount();
            bits = draw.value();
            value = bits % bound;
        } while (bits - value + (bound - 1) < 0);
        return new Draw<>(value, state, draws);
    }

    /** Equivalent to {@link net.minecraft.util.RandomSource#nextLong()}. */
    public Draw<Long> nextLong() {
        Draw<Integer> high = nextBits(32);
        Draw<Integer> low = high.state().nextBits(32);
        long value = ((long) high.value() << 32) + (long) low.value();
        return new Draw<>(value, low.state(), high.drawCount() + low.drawCount());
    }

    /** Equivalent to {@link net.minecraft.util.RandomSource#nextBoolean()}. */
    public Draw<Boolean> nextBoolean() {
        Draw<Integer> draw = nextBits(1);
        return new Draw<>(draw.value() != 0, draw.state(), draw.drawCount());
    }

    /** Equivalent to {@link net.minecraft.util.RandomSource#nextFloat()}. */
    public Draw<Float> nextFloat() {
        Draw<Integer> draw = nextBits(24);
        return new Draw<>(draw.value() * 0x1.0p-24F, draw.state(), draw.drawCount());
    }

    /** Equivalent to {@link net.minecraft.util.RandomSource#nextDouble()}. */
    public Draw<Double> nextDouble() {
        Draw<Integer> first = nextBits(26);
        Draw<Integer> second = first.state().nextBits(27);
        long combined = ((long) first.value() << 27) + (long) second.value();
        return new Draw<>(combined * 0x1.0p-53D, second.state(), 2);
    }

    public Draw<Integer> nextIntInclusive(int min, int max) {
        long size = (long) max - min + 1L;
        if (size <= 0L || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid inclusive range");
        }
        Draw<Integer> draw = nextInt((int) size);
        return new Draw<>(min + draw.value(), draw.state(), draw.drawCount());
    }

    public record Draw<T>(T value, LegacyState1201 state, int drawCount) {}
}
