package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;

import com.suntide_20210418.dimensiontech.loot.expectation.LootAnalysisContext;
import com.suntide_20210418.dimensiontech.loot.expectation.StatefulCondition1201;
import com.suntide_20210418.dimensiontech.loot.expectation.XoroshiroState1201;
import org.junit.jupiter.api.Test;

import java.util.Map;

class StatefulCondition1201Test {
    private static final LootAnalysisContext CONTEXT =
            new LootAnalysisContext(null, null, null, 0.0F, Map.of(), null, null, null, Map.of());

    @Test
    void missingLastDamagePlayerParamMakesKilledByPlayerFalseWithoutRandomCall() {
        XoroshiroState1201 initial = new XoroshiroState1201(3L, 5L);

        var result =
                StatefulCondition1201.test(
                        JsonParser.parseString("{\"condition\":\"minecraft:killed_by_player\"}"),
                        CONTEXT,
                        initial);

        assertEquals(false, result.value());
        assertEquals(initial, result.randomState());
    }

    @Test
    void vanillaPathInAnotherNamespaceRemainsUnknown() {
        var result =
                StatefulCondition1201.test(
                        JsonParser.parseString(
                                "{\"condition\":\"example:random_chance\",\"chance\":1.0}"),
                        CONTEXT,
                        new XoroshiroState1201(5L, 8L));

        assertEquals(null, result);
    }

    @Test
    void allOfShortCircuitsAfterFirstFalseWithoutConsumingLaterRandomCall() {
        XoroshiroState1201 initial = new XoroshiroState1201(13L, 21L);
        var conditions =
                JsonParser.parseString(
                        "["
                                + "{\"condition\":\"minecraft:always_false\"},"
                                + "{\"condition\":\"minecraft:random_chance\",\"chance\":0.5}]");

        var result = StatefulCondition1201.testAll(conditions, CONTEXT, initial);
        assertEquals(false, result.value());
        assertEquals(initial, result.randomState());
    }

    @Test
    void anyOfStopsAfterSuccessfulRandomTermAndPreservesExactContinuation() {
        XoroshiroState1201 initial = new XoroshiroState1201(34L, 55L);
        var condition =
                JsonParser.parseString(
                        "{"
                                + "\"condition\":\"minecraft:any_of\",\"terms\":["
                                + "{\"condition\":\"minecraft:random_chance\",\"chance\":1.0},"
                                + "{\"condition\":\"minecraft:random_chance\",\"chance\":1.0}]}");

        var result = StatefulCondition1201.test(condition, CONTEXT, initial);
        assertEquals(true, result.value());
        assertEquals(initial.nextFloat().state(), result.randomState());
    }

    @Test
    void invertedConsumesExactlyTheNestedConditionCall() {
        XoroshiroState1201 initial = new XoroshiroState1201(89L, 144L);
        var condition =
                JsonParser.parseString(
                        "{\"condition\":\"minecraft:inverted\","
                            + "\"term\":{\"condition\":\"minecraft:random_chance\",\"chance\":0.0}}");

        var result = StatefulCondition1201.test(condition, CONTEXT, initial);
        assertEquals(true, result.value());
        assertEquals(initial.nextFloat().state(), result.randomState());
    }

    @Test
    void referenceUsesResolverContinuationInsideShortCircuitComposition() {
        XoroshiroState1201 initial = new XoroshiroState1201(233L, 377L);
        var condition =
                JsonParser.parseString(
                        "{\"condition\":\"minecraft:all_of\",\"terms\":["
                            + "{\"condition\":\"minecraft:reference\",\"name\":\"example:test\"},"
                            + "{\"condition\":\"minecraft:random_chance\",\"chance\":1.0}]}");
        var referencedDraw = initial.nextBoolean();

        var result =
                StatefulCondition1201.test(
                        condition,
                        CONTEXT,
                        initial,
                        (id, state) -> {
                            var draw = state.nextBoolean();
                            return new StatefulCondition1201.Result(draw.value(), draw.state());
                        });

        assertEquals(referencedDraw.value(), result.value());
        assertEquals(
                referencedDraw.value()
                        ? referencedDraw.state().nextFloat().state()
                        : referencedDraw.state(),
                result.randomState());
    }

    @Test
    void survivesExplosionWithoutRadiusIsTrueAndDoesNotDraw() {
        XoroshiroState1201 initial = new XoroshiroState1201(610L, 987L);
        var condition = JsonParser.parseString("{\"condition\":\"minecraft:survives_explosion\"}");

        var result = StatefulCondition1201.test(condition, CONTEXT, initial);

        assertEquals(true, result.value());
        assertEquals(initial, result.randomState());
    }

    @Test
    void survivesExplosionUsesRadiusComparisonAndOneNextFloat() {
        XoroshiroState1201 initial = new XoroshiroState1201(1597L, 2584L);
        LootAnalysisContext explosionContext =
                new LootAnalysisContext(
                        null, null, null, 0.0F, Map.of(), null, 4.0F, null, Map.of());
        var condition = JsonParser.parseString("{\"condition\":\"minecraft:survives_explosion\"}");
        var draw = initial.nextFloat();

        var result = StatefulCondition1201.test(condition, explosionContext, initial);

        assertEquals(draw.value() <= 0.25F, result.value());
        assertEquals(draw.state(), result.randomState());
    }

    @Test
    void randomChanceWithLootingMatchesAdjustedThresholdAndConcreteContinuation() {
        XoroshiroState1201 initial = new XoroshiroState1201(4181L, 6765L);
        LootAnalysisContext context = CONTEXT.withLootingModifier(2);
        var condition =
                JsonParser.parseString(
                        "{\"condition\":\"minecraft:random_chance_with_looting\","
                                + "\"chance\":0.1,\"looting_multiplier\":0.2}");
        var draw = initial.nextFloat();

        var result = StatefulCondition1201.test(condition, context, initial);

        assertEquals(draw.value() < 0.5F, result.value());
        assertEquals(draw.state(), result.randomState());
    }

    @Test
    void resolverRuntimeFailureIsContainedAsUnsupported() {
        var condition =
                JsonParser.parseString(
                        "{\"condition\":\"minecraft:reference\",\"name\":\"example:test\"}");

        var result =
                StatefulCondition1201.test(
                        condition,
                        CONTEXT,
                        new XoroshiroState1201(1L, 2L),
                        (id, state) -> {
                            throw new IllegalStateException("broken predicate AST");
                        });

        assertEquals(null, result);
    }
}
