package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.loot.expectation.XoroshiroState1201;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import org.junit.jupiter.api.Test;

class XoroshiroSeed1201Test {
    @Test
    void fromSeedUsesMinecraftUpgradeAndMixExactly() {
        for (long seed : new long[] {0L, 1L, -1L, 0x123456789abcdef0L, Long.MIN_VALUE}) {
            XoroshiroState1201 state = XoroshiroState1201.fromSeed(seed);
            XoroshiroRandomSource runtime = new XoroshiroRandomSource(seed);

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

            assertEquals(runtime.nextLong(), state.nextLong().value());
        }
    }

    @Test
    void boundedRejectionReportsEveryConsumedXoroshiroTransition() {
        int bound = 1_500_000_000;
        for (long seed = 0L; seed < 100L; seed++) {
            XoroshiroState1201 state = XoroshiroState1201.fromSeed(seed);
            var draw = state.nextInt(bound);
            if (draw.drawCount() <= 1) continue;

            XoroshiroRandomSource runtime = new XoroshiroRandomSource(seed);
            assertEquals(runtime.nextInt(bound), draw.value());
            assertEquals(runtime.nextLong(), draw.state().nextLong().value());
            assertTrue(draw.drawCount() > 1);
            return;
        }
        throw new AssertionError("No Xoroshiro rejection witness found");
    }
}
