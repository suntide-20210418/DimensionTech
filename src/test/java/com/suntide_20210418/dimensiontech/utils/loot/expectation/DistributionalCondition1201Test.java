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

class DistributionalCondition1201Test {
    private static final LootAnalysisContext CONTEXT =
            new LootAnalysisContext(null, null, null, 0.0F, Map.of(), null, null, null, Map.of());

    @Test
    void zeroMassContinuationSkipsUnknownCondition() {
        var conditions =
                JsonParser.parseString(
                        "["
                                + "{\"condition\":\"minecraft:random_chance\",\"chance\":0.0},"
                                + "{\"condition\":\"example:unknown\"}]");

        var result = DistributionalCondition1201.testAll(conditions, CONTEXT, 100, "/conditions");

        assertTrue(result.supported());
        assertEquals(
                Map.of(false, ExactProbability.ONE), result.distribution().marginal().masses());
        assertEquals(
                List.of(ExactRandomSemantics1201.RandomMethod.NEXT_FLOAT),
                result.distribution().masses().keySet().iterator().next().calls().stream()
                        .map(ExactRandomSemantics1201.RandomCall::method)
                        .toList());
    }

    @Test
    void positiveMassContinuationRejectsUnknownCondition() {
        var conditions =
                JsonParser.parseString(
                        "["
                                + "{\"condition\":\"minecraft:random_chance\",\"chance\":0.5},"
                                + "{\"condition\":\"example:unknown\"}]");

        var result = DistributionalCondition1201.testAll(conditions, CONTEXT, 100, "/conditions");

        assertFalse(result.supported());
        assertEquals("/conditions/1", result.pointer());
    }

    @Test
    void vanillaPathInAnotherNamespaceRemainsUnknown() {
        var condition =
                JsonParser.parseString("{\"condition\":\"example:random_chance\",\"chance\":0.0}");

        var result = DistributionalCondition1201.test(condition, CONTEXT, 100, "/condition", null);

        assertFalse(result.supported());
        assertEquals("/condition", result.pointer());
    }

    @Test
    void anyOfShortCircuitsSuccessfulBranchesAndPreservesCallOrder() {
        var conditions =
                JsonParser.parseString(
                        "[{\"condition\":\"minecraft:any_of\",\"terms\":["
                                + "{\"condition\":\"minecraft:random_chance\",\"chance\":1.0},"
                                + "{\"condition\":\"example:unknown\"}]}]");

        var result = DistributionalCondition1201.testAll(conditions, CONTEXT, 100, "/conditions");

        assertTrue(result.supported());
        assertEquals(Map.of(true, ExactProbability.ONE), result.distribution().marginal().masses());
        assertEquals(1, result.distribution().masses().keySet().iterator().next().calls().size());
    }

    @Test
    void andBranchesRetainOneOrTwoOrderedFloatCalls() {
        var conditions =
                JsonParser.parseString(
                        "["
                                + "{\"condition\":\"minecraft:random_chance\",\"chance\":0.5},"
                                + "{\"condition\":\"minecraft:random_chance\",\"chance\":0.5}]");

        var result = DistributionalCondition1201.testAll(conditions, CONTEXT, 100, "/conditions");

        assertTrue(result.supported());
        assertEquals(
                ExactProbability.of(1, 4), result.distribution().marginal().masses().get(true));
        assertEquals(
                ExactProbability.of(3, 4), result.distribution().marginal().masses().get(false));
        assertTrue(
                result.distribution().masses().keySet().stream()
                        .allMatch(
                                outcome ->
                                        outcome.calls().size() == 1
                                                || outcome.calls().size() == 2));
    }

    @Test
    void missingLastDamagePlayerParamMakesKilledByPlayerFalse() {
        var condition = JsonParser.parseString("{\"condition\":\"minecraft:killed_by_player\"}");

        var result = DistributionalCondition1201.test(condition, CONTEXT, 100, "/condition", null);

        assertTrue(result.supported());
        assertEquals(
                Map.of(false, ExactProbability.ONE), result.distribution().marginal().masses());
        assertTrue(result.distribution().masses().keySet().iterator().next().calls().isEmpty());
    }

    @Test
    void randomChanceWithLootingUsesAdjustedFloatThresholdAndOneCall() {
        LootAnalysisContext context = CONTEXT.withLootingModifier(2);
        var condition =
                JsonParser.parseString(
                        "{\"condition\":\"minecraft:random_chance_with_looting\","
                                + "\"chance\":0.1,\"looting_multiplier\":0.2}");

        var result = DistributionalCondition1201.test(condition, context, 100, "/condition", null);

        assertTrue(result.supported());
        assertEquals(
                ExactProbability.of(1, 2), result.distribution().marginal().masses().get(true));
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
                                                                                .NEXT_FLOAT,
                                                                        0)))));
    }

    @Test
    void malformedNumericPrimitiveReturnsStructuredFieldFailure() {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:random_chance");
        condition.add("chance", new JsonPrimitive(new ThrowingNumber()));

        var result =
                DistributionalCondition1201.test(
                        condition, CONTEXT, 100, "/conditions/2", null);

        assertFalse(result.supported());
        assertEquals("/conditions/2/chance", result.pointer());
        assertEquals(EvaluationFailureKind.UNSUPPORTED_TYPE, result.failureKind());
    }

    @Test
    void resolverRuntimeFailureDoesNotEscapeConditionBoundary() {
        var condition =
                JsonParser.parseString(
                        "{\"condition\":\"minecraft:reference\",\"name\":\"example:test\"}");

        var result =
                DistributionalCondition1201.test(
                        condition,
                        CONTEXT,
                        100,
                        "/conditions/3",
                        (id, maxStates, pointer) -> {
                            throw new IllegalStateException("broken predicate AST");
                        });

        assertFalse(result.supported());
        assertEquals("/conditions/3", result.pointer());
        assertEquals(EvaluationFailureKind.UNSUPPORTED_TYPE, result.failureKind());
        assertTrue(result.message().startsWith("Malformed reachable condition AST"));
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
