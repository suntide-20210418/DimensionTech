package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class DistributionalNumberProvider1201Test {
    private static final LootAnalysisContext ZERO_LUCK =
            new LootAnalysisContext(null, null, null, 0.0F, Map.of(), null, null, null, Map.of());

    @Test
    void uniformIntegerUsesOneBoundedCallWithInclusiveEndpoints() {
        var provider =
                JsonParser.parseString("{\"type\":\"minecraft:uniform\",\"min\":2,\"max\":4}");

        var result = DistributionalNumberProvider1201.getInt(provider, ZERO_LUCK, 100, "/rolls");

        assertTrue(result.supported());
        assertEquals(
                Map.of(
                        2, ExactProbability.of(1, 3),
                        3, ExactProbability.of(1, 3),
                        4, ExactProbability.of(1, 3)),
                result.distribution().marginal().masses());
        assertTrue(
                result.distribution().masses().keySet().stream()
                        .allMatch(
                                outcome ->
                                        outcome.calls()
                                                .equals(
                                                        List.of(
                                                                new ExactRandomSemantics1201
                                                                        .RandomCall(
                                                                        ExactRandomSemantics1201
                                                                                .RandomMethod
                                                                                .NEXT_INT_BOUND,
                                                                        3)))));
    }

    @Test
    void zeroLuckUniformBonusIsZeroButRetainsItsFloatCall() {
        var provider =
                JsonParser.parseString("{\"type\":\"minecraft:uniform\",\"min\":0.0,\"max\":1.0}");

        var result =
                DistributionalNumberProvider1201.getBonusFloor(
                        provider, ZERO_LUCK, 100, "/bonus_rolls");

        assertTrue(result.supported());
        assertEquals(Map.of(0, ExactProbability.ONE), result.distribution().marginal().masses());
        assertEquals(
                List.of(
                        new ExactRandomSemantics1201.RandomCall(
                                ExactRandomSemantics1201.RandomMethod.NEXT_FLOAT, 0)),
                result.distribution().masses().keySet().iterator().next().calls());
    }

    @Test
    void binomialRetainsOneFloatCallPerTrial() {
        var provider =
                JsonParser.parseString("{\"type\":\"minecraft:binomial\",\"n\":2,\"p\":0.5}");

        var result = DistributionalNumberProvider1201.getInt(provider, ZERO_LUCK, 100, "/rolls");

        assertTrue(result.supported());
        assertEquals(ExactProbability.of(1, 2), result.distribution().marginal().masses().get(1));
        assertTrue(
                result.distribution().masses().keySet().stream()
                        .allMatch(outcome -> outcome.calls().size() == 2));
    }

    @Test
    void vanillaPathInAnotherNamespaceRemainsUnknown() {
        var provider = JsonParser.parseString("{\"type\":\"example:uniform\",\"min\":2,\"max\":4}");

        var result = DistributionalNumberProvider1201.getInt(provider, ZERO_LUCK, 100, "/rolls");

        assertEquals(false, result.supported());
        assertEquals("/rolls", result.pointer());
        assertEquals(EvaluationFailureKind.UNSUPPORTED_TYPE, result.failureKind());
    }

    @Test
    void stateSpaceLimitIsReportedAsRandomSemantics() {
        var provider =
                JsonParser.parseString("{\"type\":\"minecraft:uniform\",\"min\":2,\"max\":4}");

        var result = DistributionalNumberProvider1201.getInt(provider, ZERO_LUCK, 2, "/rolls");

        assertEquals(false, result.supported());
        assertEquals("/rolls", result.pointer());
        assertEquals(EvaluationFailureKind.RANDOM_SEMANTICS, result.failureKind());
    }

    @Test
    void malformedConstantNumberReturnsStructuredValueFailure() {
        JsonObject provider = new JsonObject();
        provider.addProperty("type", "minecraft:constant");
        provider.add("value", new JsonPrimitive(new ThrowingNumber()));

        var result =
                DistributionalNumberProvider1201.getInt(provider, ZERO_LUCK, 100, "/rolls");

        assertFalse(result.supported());
        assertEquals("/rolls/value", result.pointer());
        assertEquals(EvaluationFailureKind.UNSUPPORTED_TYPE, result.failureKind());
    }

    @Test
    void malformedUniformBonusReportsTheFailingBound() {
        var provider =
                JsonParser.parseString(
                        "{\"type\":\"minecraft:uniform\",\"min\":{},\"max\":1.0}");

        var result =
                DistributionalNumberProvider1201.getBonusFloor(
                        provider, ZERO_LUCK, 100, "/bonus_rolls");

        assertFalse(result.supported());
        assertEquals("/bonus_rolls/min", result.pointer());
        assertEquals(EvaluationFailureKind.UNSUPPORTED_TYPE, result.failureKind());
    }

    private static final class ThrowingNumber extends Number {
        @Override
        public int intValue() {
            throw new NumberFormatException("malformed number");
        }

        @Override
        public long longValue() {
            throw new NumberFormatException("malformed number");
        }

        @Override
        public float floatValue() {
            throw new NumberFormatException("malformed number");
        }

        @Override
        public double doubleValue() {
            throw new NumberFormatException("malformed number");
        }
    }
}
