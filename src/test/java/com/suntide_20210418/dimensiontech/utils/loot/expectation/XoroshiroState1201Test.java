package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.junit.jupiter.api.Test;

class XoroshiroState1201Test {
    @Test
    void deterministicTransitionsMatchMinecraftAcrossMixedCalls() {
        long low = 0x0123456789abcdefL;
        long high = 0xfedcba9876543210L;
        XoroshiroState1201 state = new XoroshiroState1201(low, high);
        XoroshiroRandomSource runtime = new XoroshiroRandomSource(low, high);

        var bounded = state.nextInt(137);
        assertEquals(runtime.nextInt(137), bounded.value());
        state = bounded.state();

        var floatDraw = state.nextFloat();
        assertEquals(Float.floatToRawIntBits(runtime.nextFloat()),
                Float.floatToRawIntBits(floatDraw.value()));
        state = floatDraw.state();

        var doubleDraw = state.nextDouble();
        assertEquals(Double.doubleToRawLongBits(runtime.nextDouble()),
                Double.doubleToRawLongBits(doubleDraw.value()));
        state = doubleDraw.state();

        var booleanDraw = state.nextBoolean();
        assertEquals(runtime.nextBoolean(), booleanDraw.value());
        state = booleanDraw.state();

        var longDraw = state.nextLong();
        assertEquals(runtime.nextLong(), longDraw.value());
    }

    @Test
    void zeroStateUsesMinecraftNonzeroFallback() {
        XoroshiroState1201 state = new XoroshiroState1201(0L, 0L);
        XoroshiroRandomSource runtime = new XoroshiroRandomSource(0L, 0L);

        assertEquals(runtime.nextLong(), state.nextLong().value());
    }

    @Test
    void boundedCallsMatchRuntimeForDifficultBoundsAndPreserveContinuation() {
        int[] bounds = {1, 2, 3, 7, 137, 65537, Integer.MAX_VALUE};
        XoroshiroState1201 state = new XoroshiroState1201(-1L, 42L);
        XoroshiroRandomSource runtime = new XoroshiroRandomSource(-1L, 42L);
        for (int bound : bounds) {
            var draw = state.nextInt(bound);
            assertEquals(runtime.nextInt(bound), draw.value());
            state = draw.state();
        }
        assertEquals(runtime.nextLong(), state.nextLong().value());
    }

    @Test
    void inclusiveIntegerMatchesMthNextIntAndCarriesContinuation() {
        XoroshiroState1201 state = new XoroshiroState1201(91L, -73L);
        XoroshiroRandomSource runtime = new XoroshiroRandomSource(91L, -73L);

        var draw = state.nextIntInclusive(-4, 9);
        assertEquals(net.minecraft.util.Mth.nextInt(runtime, -4, 9), draw.value());
        assertEquals(runtime.nextLong(), draw.state().nextLong().value());
    }
}
