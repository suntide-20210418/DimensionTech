package com.suntide_20210418.dimensiontech.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class EnchantmentMarginalTest {
    private static EnchantmentKey key(String path, int level) {
        return new EnchantmentKey(ResourceLocation.fromNamespaceAndPath("minecraft", path), level);
    }

    @Test
    void emptyIsTheIdentityForPlus() {
        EnchantmentMarginal marginal = EnchantmentMarginal.of(Map.of(key("sharpness", 1), ExactProbability.of(3, 2)));
        assertEquals(marginal, EnchantmentMarginal.EMPTY.plus(marginal));
        assertEquals(marginal, marginal.plus(EnchantmentMarginal.EMPTY));
    }

    @Test
    void plusAccumulatesTheSameKeyExactly() {
        EnchantmentMarginal left = EnchantmentMarginal.of(Map.of(key("sharpness", 2), ExactProbability.of(1, 3)));
        EnchantmentMarginal right = EnchantmentMarginal.of(Map.of(key("sharpness", 2), ExactProbability.of(1, 6)));
        assertEquals(
                ExactProbability.of(1, 2),
                left.plus(right).values().get(key("sharpness", 2)));
    }

    @Test
    void differentLevelsOfOneEnchantmentStaySeparate() {
        Map<EnchantmentKey, ExactProbability> values = new LinkedHashMap<>();
        values.put(key("sharpness", 1), ExactProbability.ONE);
        values.put(key("sharpness", 5), ExactProbability.ONE);
        EnchantmentMarginal marginal = EnchantmentMarginal.of(values);
        assertEquals(2, marginal.values().size());
        assertEquals(ExactProbability.ONE, marginal.values().get(key("sharpness", 5)));
    }

    @Test
    void scalingByZeroDropsEverything() {
        EnchantmentMarginal marginal = EnchantmentMarginal.of(Map.of(key("sharpness", 3), ExactProbability.of(5, 7)));
        assertTrue(marginal.scale(ExactProbability.ZERO).isEmpty());
        assertTrue(EnchantmentMarginal.EMPTY.scale(ExactProbability.ONE).isEmpty());
    }

    @Test
    void scaleKeepsExactRationalArithmetic() {
        EnchantmentMarginal marginal = EnchantmentMarginal.of(Map.of(key("sharpness", 3), ExactProbability.of(2, 3)));
        assertEquals(
                ExactProbability.of(1, 3),
                marginal.scale(ExactProbability.of(1, 2)).values().get(key("sharpness", 3)));
    }

    @Test
    void scaleSaturatingClampsInsteadOfOverflowing() {
        EnchantmentMarginal marginal = EnchantmentMarginal.of(Map.of(key("sharpness", 3), ExactProbability.of(7, 3)));
        EnchantmentMarginal scaled = marginal.scaleSaturating(ExactProbability.of(Integer.MAX_VALUE, 1));
        assertEquals(
                ExactProbability.of(Integer.MAX_VALUE, 1),
                scaled.values().get(key("sharpness", 3)));
    }

    @Test
    void zeroMassIsDroppedOnConstruction() {
        Map<EnchantmentKey, ExactProbability> values = new LinkedHashMap<>();
        values.put(key("sharpness", 1), ExactProbability.ZERO);
        assertTrue(EnchantmentMarginal.of(values).isEmpty());
    }

    @Test
    void keyRejectsNonPositiveLevel() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> key("sharpness", 0));
    }
}
