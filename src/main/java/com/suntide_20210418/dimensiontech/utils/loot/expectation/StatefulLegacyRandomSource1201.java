package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.MarsagliaPolarGaussian;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** Mutable {@link RandomSource} adapter backed by an exact {@link LegacyState1201} continuation. */
public final class StatefulLegacyRandomSource1201 implements RandomSource {
    private LegacyState1201 state;
    private final MarsagliaPolarGaussian gaussian = new MarsagliaPolarGaussian(this);

    /**
     * Creates an adapter with the same user-facing seed semantics as {@code LegacyRandomSource}.
     */
    public StatefulLegacyRandomSource1201(long seed) {
        this(LegacyState1201.fromSeed(seed));
    }

    public StatefulLegacyRandomSource1201(LegacyState1201 state) {
        this.state = java.util.Objects.requireNonNull(state, "state");
    }

    public LegacyState1201 state() {
        return state;
    }

    @Override
    public RandomSource fork() {
        return new StatefulLegacyRandomSource1201(LegacyState1201.fromSeed(nextLong()));
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        // This is the exact factory class returned by LegacyRandomSource.forkPositional().
        return new LegacyRandomSource.LegacyPositionalRandomFactory(nextLong());
    }

    @Override
    public void setSeed(long seed) {
        state = LegacyState1201.fromSeed(seed);
        gaussian.reset();
    }

    @Override
    public int nextInt() {
        var draw = state.nextInt();
        state = draw.state();
        return draw.value();
    }

    @Override
    public int nextInt(int bound) {
        var draw = state.nextInt(bound);
        state = draw.state();
        return draw.value();
    }

    @Override
    public long nextLong() {
        var draw = state.nextLong();
        state = draw.state();
        return draw.value();
    }

    @Override
    public boolean nextBoolean() {
        var draw = state.nextBoolean();
        state = draw.state();
        return draw.value();
    }

    @Override
    public float nextFloat() {
        var draw = state.nextFloat();
        state = draw.state();
        return draw.value();
    }

    @Override
    public double nextDouble() {
        var draw = state.nextDouble();
        state = draw.state();
        return draw.value();
    }

    @Override
    public double nextGaussian() {
        return gaussian.nextGaussian();
    }

    @Override
    public void consumeCount(int count) {
        // LegacyRandomSource inherits RandomSource's nextInt()-based default implementation.
        for (int index = 0; index < count; index++) nextInt();
    }
}
