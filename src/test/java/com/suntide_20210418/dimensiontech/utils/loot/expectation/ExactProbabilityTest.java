package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

class ExactProbabilityTest {
    @Test
    void idealMarginalArithmeticReducesAndCombinesWithoutRounding() {
        ExactProbability expectedRolls = ExactProbability.of(19, 2);
        ExactProbability selection = ExactProbability.of(12, 137);
        ExactProbability stackCount = ExactProbability.of(25, 1);
        ExactProbability selectedStacks = expectedRolls.multiply(selection);
        ExactProbability value = selectedStacks.multiply(stackCount);

        assertEquals(ExactProbability.of(19, 2), expectedRolls);
        assertEquals(ExactProbability.of(12, 137), selection);
        assertEquals(ExactProbability.of(25, 1), stackCount);
        assertEquals(ExactProbability.of(114, 137), selectedStacks);
        assertEquals(ExactProbability.of(2850, 137), value);
        assertEquals(20.802919708029197D, value.doubleValue());
    }

    @Test
    void finiteDistributionRequiresAnExactUnitMass() {
        Map<String, ExactProbability> masses = new LinkedHashMap<>();
        masses.put("a", ExactProbability.of(1, 3));
        masses.put("b", ExactProbability.of(2, 3));

        FiniteDistribution<String> distribution = FiniteDistribution.of(masses);
        assertEquals(
                ExactProbability.ONE,
                distribution.masses().values().stream()
                        .reduce(ExactProbability.ZERO, ExactProbability::add));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        FiniteDistribution.of(
                                Map.of(
                                        "a",
                                        ExactProbability.of(1, 3),
                                        "b",
                                        ExactProbability.of(1, 3))));
    }

    @Test
    void doubleConversionDoesNotTurnAFiniteHugeRatioIntoNan() {
        BigInteger scale = BigInteger.ONE.shiftLeft(2_000);
        ExactProbability ratio = ExactProbability.of(scale.add(BigInteger.ONE), scale);

        assertEquals(1.0D, ratio.doubleValue());
        assertEquals(1.0D, ratio.finiteDoubleValue());
    }

    @Test
    void finiteDoubleConversionRejectsAnUnrepresentableExactValue() {
        ExactProbability tooLarge =
                ExactProbability.of(BigInteger.ONE.shiftLeft(1024), BigInteger.ONE);

        assertEquals(Double.POSITIVE_INFINITY, tooLarge.doubleValue());
        assertThrows(ArithmeticException.class, tooLarge::finiteDoubleValue);
    }

    @Test
    void doubleConversionUsesNearestEvenAtNormalAndSubnormalTies() {
        ExactProbability normalTie =
                ExactProbability.of(
                        BigInteger.ONE.shiftLeft(53).add(BigInteger.ONE),
                        BigInteger.ONE.shiftLeft(53));
        ExactProbability subnormalTieDown =
                ExactProbability.of(BigInteger.ONE, BigInteger.ONE.shiftLeft(1075));
        ExactProbability subnormalTieUp =
                ExactProbability.of(BigInteger.valueOf(3), BigInteger.ONE.shiftLeft(1075));

        assertEquals(1.0D, normalTie.doubleValue());
        assertEquals(0.0D, subnormalTieDown.doubleValue());
        assertEquals(Double.longBitsToDouble(2L), subnormalTieUp.doubleValue());
    }
}
