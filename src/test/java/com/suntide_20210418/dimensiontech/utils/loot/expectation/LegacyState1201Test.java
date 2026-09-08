package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.loot.expectation.LegacyState1201;
import com.suntide_20210418.dimensiontech.loot.expectation.StatefulLegacyRandomSource1201;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

import org.junit.jupiter.api.Test;

class LegacyState1201Test {
    @Test
    void mixedRuntimeCallsAndContinuationMatchMinecraft() {
        long seed = 0x123456789abcdef0L;
        LegacyState1201 state = LegacyState1201.fromSeed(seed);
        LegacyRandomSource runtime = new LegacyRandomSource(seed);

        var bounded = state.nextInt(137);
        assertEquals(runtime.nextInt(137), bounded.value());
        state = bounded.state();

        var floatDraw = state.nextFloat();
        assertEquals(
                Float.floatToRawIntBits(runtime.nextFloat()),
                Float.floatToRawIntBits(floatDraw.value()));
        state = floatDraw.state();

        var doubleDraw = state.nextDouble();
        assertEquals(
                Double.doubleToRawLongBits(runtime.nextDouble()),
                Double.doubleToRawLongBits(doubleDraw.value()));
        state = doubleDraw.state();

        var booleanDraw = state.nextBoolean();
        assertEquals(runtime.nextBoolean(), booleanDraw.value());
        state = booleanDraw.state();

        var longDraw = state.nextLong();
        assertEquals(runtime.nextLong(), longDraw.value());
        assertEquals(runtime.nextInt(), longDraw.state().nextInt().value());
    }

    @Test
    void boundedPowerOfTwoAndRejectionPathsPreserveContinuation() {
        int[] bounds = {1, 2, 3, 7, 16, 137, 65537, Integer.MAX_VALUE};
        LegacyState1201 state = LegacyState1201.fromSeed(-0x123456789L);
        LegacyRandomSource runtime = new LegacyRandomSource(-0x123456789L);
        for (int bound : bounds) {
            var draw = state.nextInt(bound);
            assertEquals(runtime.nextInt(bound), draw.value());
            state = draw.state();
        }
        assertEquals(runtime.nextLong(), state.nextLong().value());
    }

    @Test
    void boundedRejectionReportsEveryConsumedLcgTransition() {
        int bound = 1_500_000_000;
        for (long seed = 0L; seed < 100L; seed++) {
            LegacyState1201 state = LegacyState1201.fromSeed(seed);
            var draw = state.nextInt(bound);
            if (draw.drawCount() <= 1) continue;

            LegacyRandomSource runtime = new LegacyRandomSource(seed);
            assertEquals(runtime.nextInt(bound), draw.value());
            assertEquals(runtime.nextLong(), draw.state().nextLong().value());
            assertTrue(draw.drawCount() > 1);
            return;
        }
        throw new AssertionError("No LegacyRandomSource rejection witness found");
    }

    @Test
    void adapterMatchesRuntimeReseedGaussianForkAndConsumption() {
        StatefulLegacyRandomSource1201 actual =
                new StatefulLegacyRandomSource1201(LegacyState1201.fromSeed(3L));
        LegacyRandomSource expected = new LegacyRandomSource(3L);

        assertEquals(expected.nextGaussian(), actual.nextGaussian());
        assertEquals(expected.nextGaussian(), actual.nextGaussian());
        actual.setSeed(987654321L);
        expected.setSeed(987654321L);
        assertEquals(expected.nextGaussian(), actual.nextGaussian());

        RandomSourcePair pair = new RandomSourcePair(actual.fork(), expected.fork());
        assertEquals(pair.expected.nextLong(), pair.actual.nextLong());
        assertEquals(pair.expected.nextInt(137), pair.actual.nextInt(137));

        var actualPositional = actual.forkPositional();
        var expectedPositional = expected.forkPositional();
        assertEquals(
                expectedPositional.at(11, -7, 29).nextLong(),
                actualPositional.at(11, -7, 29).nextLong());
        assertEquals(
                expectedPositional.fromHashOf("dimension_tech").nextLong(),
                actualPositional.fromHashOf("dimension_tech").nextLong());

        actual.consumeCount(3);
        expected.consumeCount(3);
        assertEquals(expected.nextLong(), actual.nextLong());
    }

    private record RandomSourcePair(
            net.minecraft.util.RandomSource actual, net.minecraft.util.RandomSource expected) {}
}
