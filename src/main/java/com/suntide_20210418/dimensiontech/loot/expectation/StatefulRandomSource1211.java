package com.suntide_20210418.dimensiontech.loot.expectation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.MarsagliaPolarGaussian;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

/** Mutable RandomSource adapter whose exact continuation can be recovered after runtime helpers. */
public final class StatefulRandomSource1211 implements RandomSource {
    private XoroshiroState1211 state;
    private final MarsagliaPolarGaussian gaussian = new MarsagliaPolarGaussian(this);

    public StatefulRandomSource1211(XoroshiroState1211 state) {
        this.state = java.util.Objects.requireNonNull(state, "state");
    }

    public XoroshiroState1211 state() {
        return state;
    }

    @Override
    public RandomSource fork() {
        return new StatefulRandomSource1211(new XoroshiroState1211(nextLong(), nextLong()));
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return new XoroshiroRandomSource.XoroshiroPositionalRandomFactory(nextLong(), nextLong());
    }

    @Override
    public void setSeed(long seed) {
        state = XoroshiroState1211.fromSeed(seed);
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
        for (int index = 0; index < count; index++) nextLong();
    }
}
