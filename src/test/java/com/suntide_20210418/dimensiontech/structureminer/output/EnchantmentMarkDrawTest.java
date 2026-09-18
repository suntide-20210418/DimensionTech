package com.suntide_20210418.dimensiontech.structureminer.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.loot.expectation.EnchantmentKey;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class EnchantmentMarkDrawTest {
    private static EnchantmentKey key(String path, int level) {
        return new EnchantmentKey(ResourceLocation.fromNamespaceAndPath("minecraft", path), level);
    }

    @Test
    void nonFiniteAndNonPositiveExpectationsProduceNothing() {
        RandomSource random = RandomSource.create(0x1234L);
        assertEquals(0, ExpectationRewardGenerator.randomizedCount(0.0D, random));
        assertEquals(0, ExpectationRewardGenerator.randomizedCount(-1.0D, random));
        assertEquals(0, ExpectationRewardGenerator.randomizedCount(Double.NaN, random));
        assertEquals(0, ExpectationRewardGenerator.randomizedCount(Double.POSITIVE_INFINITY, random));
    }

    @Test
    void wholeExpectationIsDeterministic() {
        RandomSource random = RandomSource.create(0x1234L);
        for (int trial = 0; trial < 64; trial++) {
            assertEquals(7, ExpectationRewardGenerator.randomizedCount(7.0D, random));
        }
    }

    /**
     * The whole point of floor plus one Bernoulli trial: the realized average converges on the
     * expectation instead of systematically under-delivering.
     */
    @Test
    void fractionalExpectationIsPreservedOnAverage() {
        double expected = 3.25D;
        RandomSource random = RandomSource.create(0x9E3779B9L);
        long samples = 400_000L;
        long total = 0L;
        for (long index = 0; index < samples; index++) {
            total += ExpectationRewardGenerator.randomizedCount(expected, random);
        }
        double average = total / (double) samples;
        assertEquals(expected, average, 0.02D);
    }

    /**
     * Not covered here: {@code totalWeight} touches {@code BuiltInRegistries.ITEM}, which is not
     * bootstrapped in a plain JUnit run. Its filtering behaviour is exercised indirectly by the
     * miner gametests.
     */
    @Test
    void hugeExpectationIsClamped() {
        RandomSource random = RandomSource.create(1L);
        assertEquals(4096, ExpectationRewardGenerator.randomizedCount(1.0e12D, random));
        assertTrue(ExpectationRewardGenerator.randomizedCount(4095.9D, random) <= 4096);
    }
}
