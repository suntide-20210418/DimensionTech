package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactRandomSemantics1201.RandomCall;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactRandomSemantics1201.RandomMethod;
import com.suntide_20210418.dimensiontech.loot.expectation.RandomTraceDistribution;
import com.suntide_20210418.dimensiontech.loot.expectation.RandomTraceDistribution.Outcome;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class RandomTraceDistributionTest {
    @Test
    void flatMapRetainsDifferentOrderedCallPathsAndMarginalizesOnlyOnRequest() {
        Map<Outcome<Boolean>, ExactProbability> first = new LinkedHashMap<>();
        first.put(
                new Outcome<>(false, List.of(new RandomCall(RandomMethod.NEXT_BOOLEAN, 0))),
                ExactProbability.of(1, 2));
        first.put(
                new Outcome<>(true, List.of(new RandomCall(RandomMethod.NEXT_BOOLEAN, 0))),
                ExactProbability.of(1, 2));

        RandomTraceDistribution<String> result =
                RandomTraceDistribution.of(first)
                        .flatMap(
                                value ->
                                        value
                                                ? RandomTraceDistribution.of(
                                                        Map.of(
                                                                new Outcome<>(
                                                                        "done",
                                                                        List.of(
                                                                                new RandomCall(
                                                                                        RandomMethod
                                                                                                .NEXT_INT_BOUND,
                                                                                        3))),
                                                                ExactProbability.ONE))
                                                : RandomTraceDistribution.singleton("done"),
                                10);

        assertEquals(2, result.masses().size());
        assertEquals(Map.of("done", ExactProbability.ONE), result.marginal().masses());
        assertEquals(
                List.of(1, 2),
                result.masses().keySet().stream()
                        .map(outcome -> outcome.calls().size())
                        .sorted()
                        .toList());
    }
}
