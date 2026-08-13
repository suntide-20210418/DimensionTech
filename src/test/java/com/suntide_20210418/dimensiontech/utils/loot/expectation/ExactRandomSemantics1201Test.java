package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExactRandomSemantics1201Test {
    @Test
    void constantIntegerProviderUsesMinecraftFloatFloorInsteadOfJavaTruncation() {
        assertEquals(
                -2,
                JsonLootTableExecutor.constantIntValue(JsonParser.parseString("-1.25")));
        assertEquals(
                16_777_216,
                JsonLootTableExecutor.constantIntValue(JsonParser.parseString("16777217")));
    }

    @Test
    void boundedNextIntIsUniformAndRecordsItsBound() {
        var result = ExactRandomSemantics1201.nextInt(3);

        assertEquals(3, result.distribution().masses().size());
        assertEquals(ExactProbability.of(1, 3), result.distribution().masses().get(0));
        assertEquals(ExactProbability.of(1, 3), result.distribution().masses().get(2));
        assertEquals(
                List.of(new ExactRandomSemantics1201.RandomCall(
                        ExactRandomSemantics1201.RandomMethod.NEXT_INT_BOUND, 3)),
                result.calls());
    }

    @Test
    void uniformIntegerUsesBoundedNextIntAndInclusiveEndpoints() {
        var result = ExactRandomSemantics1201.uniformIntInclusive(2, 4);

        assertEquals(ExactProbability.of(1, 3), result.distribution().masses().get(2));
        assertEquals(ExactProbability.of(1, 3), result.distribution().masses().get(4));
        assertEquals(1, result.calls().size());
        assertEquals(3, result.calls().get(0).bound());
    }

    @Test
    void weightedSelectionMatchesLootPoolSingleCandidateFastPath() {
        var single = ExactRandomSemantics1201.weightedIndex(List.of(37));
        var multiple = ExactRandomSemantics1201.weightedIndex(List.of(1, 3));

        assertEquals(Map.of(0, ExactProbability.ONE), single.distribution().masses());
        assertTrue(single.calls().isEmpty());
        assertEquals(ExactProbability.of(1, 4), multiple.distribution().masses().get(0));
        assertEquals(ExactProbability.of(3, 4), multiple.distribution().masses().get(1));
        assertEquals(4, multiple.calls().get(0).bound());
        assertThrows(
                IllegalArgumentException.class,
                () -> ExactRandomSemantics1201.weightedIndex(List.of(1, 0, 3)));
    }

    @Test
    void nextFloatComparisonUsesAllTwentyFourOutputBits() {
        var never = ExactRandomSemantics1201.nextFloatLessThan(0.0F).distribution();
        var always = ExactRandomSemantics1201.nextFloatLessThan(1.0F).distribution();
        var smallestPositive = ExactRandomSemantics1201
                .nextFloatLessThan(Float.intBitsToFloat(1))
                .distribution();

        assertFalse(never.masses().containsKey(true));
        assertFalse(always.masses().containsKey(false));
        assertEquals(ExactProbability.ONE, always.masses().get(true));
        assertEquals(
                ExactProbability.of(BigInteger.ONE, BigInteger.ONE.shiftLeft(24)),
                smallestPositive.masses().get(true));
    }

    @Test
    void binomialCallsNextFloatOncePerTrialInOrder() {
        var result = ExactRandomSemantics1201.binomial(2, 0.5F);

        assertEquals(ExactProbability.of(1, 4), result.distribution().masses().get(0));
        assertEquals(ExactProbability.of(1, 2), result.distribution().masses().get(1));
        assertEquals(ExactProbability.of(1, 4), result.distribution().masses().get(2));
        assertEquals(2, result.calls().size());
        assertTrue(result.calls().stream().allMatch(
                call -> call.method() == ExactRandomSemantics1201.RandomMethod.NEXT_FLOAT));
    }

    @Test
    void zeroLuckCollapsesUniformBonusWithoutRandomCall() {
        var result = ExactRandomSemantics1201.uniformFloatTimesLuckFloor(
                0.0F, 3.0F, 0.0F, 10);

        assertEquals(Map.of(0, ExactProbability.ONE), result.distribution().masses());
        assertTrue(result.calls().isEmpty());
    }

    @Test
    void uniformFloatBonusEnumeratesEveryFloatOutcomeExactly() {
        var result = ExactRandomSemantics1201.uniformFloatTimesLuckFloor(
                0.0F, 2.0F, 1.0F, 10);

        assertEquals(ExactProbability.of(1, 2), result.distribution().masses().get(0));
        assertEquals(ExactProbability.of(1, 2), result.distribution().masses().get(1));
        assertEquals(1, result.calls().size());
        assertEquals(
                ExactRandomSemantics1201.RandomMethod.NEXT_FLOAT,
                result.calls().get(0).method());
    }

    @Test
    void nextDoubleAndBooleanUseTheirOwnMethodTypes() {
        var doubleResult = ExactRandomSemantics1201.nextDoubleLessThan(0.5D);
        var booleanResult = ExactRandomSemantics1201.nextBoolean();

        assertEquals(ExactProbability.of(1, 2), doubleResult.distribution().masses().get(true));
        assertEquals(
                ExactRandomSemantics1201.RandomMethod.NEXT_DOUBLE,
                doubleResult.calls().get(0).method());
        assertEquals(ExactProbability.of(1, 2), booleanResult.distribution().masses().get(true));
        assertEquals(
                ExactRandomSemantics1201.RandomMethod.NEXT_BOOLEAN,
                booleanResult.calls().get(0).method());
    }

    @Test
    void shuffleUsesDescendingBoundedNextIntCalls() {
        var result = ExactRandomSemantics1201.shuffle(List.of("a", "b", "c"), 10);

        assertEquals(6, result.distribution().masses().size());
        assertTrue(result.distribution().masses().values().stream()
                .allMatch(mass -> mass.equals(ExactProbability.of(1, 6))));
        assertEquals(List.of(3, 2), result.calls().stream()
                .map(ExactRandomSemantics1201.RandomCall::bound)
                .toList());
    }

    @Test
    void setDamageInterpretsProviderAsRemainingDurability() {
        assertEquals(75, ExactRandomSemantics1201.setDamageValue(0.25F, false, 0, 100));
        assertEquals(0, ExactRandomSemantics1201.setDamageValue(1.0F, false, 0, 100));
        assertEquals(100, ExactRandomSemantics1201.setDamageValue(0.0F, false, 0, 100));
    }

    @Test
    void additiveSetDamageStartsFromCurrentRemainingDurability() {
        assertEquals(25, ExactRandomSemantics1201.setDamageValue(0.25F, true, 50, 100));
        assertEquals(0, ExactRandomSemantics1201.setDamageValue(0.75F, true, 50, 100));
    }

    @Test
    void degenerateUniformSetDamageDoesNotCallRandom() {
        var result = ExactRandomSemantics1201.uniformFloatSetDamage(
                0.25F, 0.25F, false, 0, 100, 101);

        assertEquals(Map.of(75, ExactProbability.ONE), result.distribution().masses());
        assertTrue(result.calls().isEmpty());
    }

    @Test
    void zeroEnchantabilitySkipsEveryEnchantmentRandomCall() {
        var result = ExactRandomSemantics1201.enchantmentLevelPerturbation(30, 0, 100);

        assertEquals(Map.of(30, ExactProbability.ONE), result.distribution().masses());
        assertTrue(result.calls().isEmpty());
    }

    @Test
    void enchantmentLevelPerturbationHasExactMassAndSourceCallOrder() {
        var result = ExactRandomSemantics1201.enchantmentLevelPerturbation(30, 10, 100);

        assertEquals(
                ExactProbability.ONE,
                result.distribution().masses().values().stream()
                        .reduce(ExactProbability.ZERO, ExactProbability::add));
        assertEquals(
                List.of(
                        ExactRandomSemantics1201.RandomMethod.NEXT_INT_BOUND,
                        ExactRandomSemantics1201.RandomMethod.NEXT_INT_BOUND,
                        ExactRandomSemantics1201.RandomMethod.NEXT_FLOAT,
                        ExactRandomSemantics1201.RandomMethod.NEXT_FLOAT),
                result.calls().stream().map(ExactRandomSemantics1201.RandomCall::method).toList());
        assertEquals(List.of(3, 3, 0, 0),
                result.calls().stream().map(ExactRandomSemantics1201.RandomCall::bound).toList());
    }
}
