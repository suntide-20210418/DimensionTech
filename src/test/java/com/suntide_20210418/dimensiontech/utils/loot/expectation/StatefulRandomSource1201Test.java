package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import org.junit.jupiter.api.Test;

class StatefulRandomSource1201Test {
    @Test
    void mixedRuntimeCallsAndContinuationMatchXoroshiroRandomSource() {
        XoroshiroState1201 initial =
                new XoroshiroState1201(0x123456789abcdef0L, 0xfedcba9876543210L);
        StatefulRandomSource1201 actual = new StatefulRandomSource1201(initial);
        XoroshiroRandomSource expected =
                new XoroshiroRandomSource(initial.seedLo(), initial.seedHi());

        assertEquals(expected.nextInt(137), actual.nextInt(137));
        assertEquals(expected.nextFloat(), actual.nextFloat());
        assertEquals(expected.nextDouble(), actual.nextDouble());
        assertEquals(expected.nextBoolean(), actual.nextBoolean());
        assertEquals(expected.nextInt(), actual.nextInt());
        assertEquals(expected.nextLong(), actual.nextLong());
        assertEquals(expected.nextLong(), actual.state().nextLong().value());
    }

    @Test
    void gaussianCacheAndReseedMatchRuntime() {
        StatefulRandomSource1201 actual =
                new StatefulRandomSource1201(new XoroshiroState1201(3L, 5L));
        XoroshiroRandomSource expected = new XoroshiroRandomSource(3L, 5L);

        assertEquals(expected.nextGaussian(), actual.nextGaussian());
        assertEquals(expected.nextGaussian(), actual.nextGaussian());
        actual.setSeed(987654321L);
        expected.setSeed(987654321L);
        assertEquals(expected.nextGaussian(), actual.nextGaussian());
        assertEquals(expected.nextLong(), actual.nextLong());
    }

    @Test
    void forkConsumesTwoLongsAndProducesMatchingChild() {
        StatefulRandomSource1201 actual =
                new StatefulRandomSource1201(new XoroshiroState1201(8L, 13L));
        XoroshiroRandomSource expected = new XoroshiroRandomSource(8L, 13L);
        var actualChild = actual.fork();
        var expectedChild = expected.fork();

        assertEquals(expectedChild.nextLong(), actualChild.nextLong());
        assertEquals(expected.nextLong(), actual.nextLong());
    }
}
