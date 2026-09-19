package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.suntide_20210418.dimensiontech.loot.expectation.StatefulRandomSource1201;
import com.suntide_20210418.dimensiontech.loot.expectation.XoroshiroState1201;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import org.junit.jupiter.api.Test;

class XoroshiroConsumeCount1201Test {
    @Test
    void consumeCountUsesRuntimeXoroshiroLongTransitions() {
        XoroshiroState1201 initial =
                new XoroshiroState1201(0x123456789abcdef0L, 0xfedcba9876543210L);
        StatefulRandomSource1201 actual = new StatefulRandomSource1201(initial);
        XoroshiroRandomSource expected =
                new XoroshiroRandomSource(initial.seedLo(), initial.seedHi());

        actual.consumeCount(7);
        expected.consumeCount(7);
        assertEquals(expected.nextLong(), actual.nextLong());

        // The runtime default loop also treats a negative count as zero iterations.
        actual.consumeCount(-1);
        expected.consumeCount(-1);
        assertEquals(expected.nextLong(), actual.nextLong());
    }
}
