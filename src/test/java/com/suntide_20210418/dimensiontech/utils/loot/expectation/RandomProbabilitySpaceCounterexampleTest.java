package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.suntide_20210418.dimensiontech.loot.expectation.*;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class RandomProbabilitySpaceCounterexampleTest {
    @Test
    void matchingCallMarginalsDoNotCertifyAConcreteXoroshiroFirstMoment() {
        XoroshiroState1201 zeroZero = stateProducing(0, 0);
        XoroshiroState1201 oneOne = stateProducing(1, 1);

        Map<List<Integer>, ExactProbability> concretePairs =
                Map.of(
                        List.of(0, 0), ExactProbability.of(1, 2),
                        List.of(1, 1), ExactProbability.of(1, 2));

        assertEquals(List.of(0, 0), drawPair(zeroZero));
        assertEquals(List.of(1, 1), drawPair(oneOne));
        assertEquals(
                Map.of(0, ExactProbability.of(1, 2), 1, ExactProbability.of(1, 2)),
                marginal(concretePairs, 0));
        assertEquals(
                Map.of(0, ExactProbability.of(1, 2), 1, ExactProbability.of(1, 2)),
                marginal(concretePairs, 1));

        FiniteDistribution<List<Integer>> idealPairs =
                RandomTraceDistribution.fromRandomResult(ExactRandomSemantics1201.nextInt(2))
                        .flatMap(
                                first ->
                                        RandomTraceDistribution.fromRandomResult(
                                                        ExactRandomSemantics1201.nextInt(2))
                                                .flatMap(
                                                        second ->
                                                                RandomTraceDistribution.singleton(
                                                                        List.of(first, second)),
                                                        4),
                                4)
                        .marginal();

        assertNotEquals(concretePairs, idealPairs.masses());
        assertEquals(ExactProbability.ONE, equalPairMass(concretePairs));
        assertEquals(ExactProbability.of(1, 2), equalPairMass(idealPairs.masses()));
    }

    private static XoroshiroState1201 stateProducing(int firstExpected, int secondExpected) {
        for (long seed = 0; seed < 10_000; seed++) {
            XoroshiroState1201 state = XoroshiroState1201.fromSeed(seed);
            if (drawPair(state).equals(List.of(firstExpected, secondExpected))) return state;
        }
        throw new AssertionError("No witness state found");
    }

    private static List<Integer> drawPair(XoroshiroState1201 state) {
        XoroshiroState1201.Draw<Integer> first = state.nextInt(2);
        XoroshiroState1201.Draw<Integer> second = first.state().nextInt(2);
        return List.of(first.value(), second.value());
    }

    private static Map<Integer, ExactProbability> marginal(
            Map<List<Integer>, ExactProbability> pairs, int index) {
        LinkedHashMap<Integer, ExactProbability> result = new LinkedHashMap<>();
        pairs.forEach(
                (pair, mass) ->
                        result.merge(pair.get(index), mass, ExactProbability::add));
        return result;
    }

    private static ExactProbability equalPairMass(
            Map<List<Integer>, ExactProbability> pairs) {
        ExactProbability result = ExactProbability.ZERO;
        for (Map.Entry<List<Integer>, ExactProbability> entry : pairs.entrySet()) {
            if (entry.getKey().get(0).equals(entry.getKey().get(1))) {
                result = result.add(entry.getValue());
            }
        }
        return result;
    }
}
