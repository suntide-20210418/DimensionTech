package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatefulCondition1201Test {
    private static final LootAnalysisContext CONTEXT = new LootAnalysisContext(
            null, null, null, 0.0F, Map.of(), null, null, null);

    @Test
    void allOfShortCircuitsAfterFirstFalseWithoutConsumingLaterRandomCall() {
        XoroshiroState1201 initial = new XoroshiroState1201(13L, 21L);
        var conditions = JsonParser.parseString("["
                + "{\"condition\":\"minecraft:always_false\"},"
                + "{\"condition\":\"minecraft:random_chance\",\"chance\":0.5}]");

        var result = StatefulCondition1201.testAll(conditions, CONTEXT, initial);
        assertEquals(false, result.value());
        assertEquals(initial, result.randomState());
    }

    @Test
    void anyOfStopsAfterSuccessfulRandomTermAndPreservesExactContinuation() {
        XoroshiroState1201 initial = new XoroshiroState1201(34L, 55L);
        var condition = JsonParser.parseString("{"
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
        var condition = JsonParser.parseString("{"
                + "\"condition\":\"minecraft:inverted\","
                + "\"term\":{\"condition\":\"minecraft:random_chance\",\"chance\":0.0}}");

        var result = StatefulCondition1201.test(condition, CONTEXT, initial);
        assertEquals(true, result.value());
        assertEquals(initial.nextFloat().state(), result.randomState());
    }
}
