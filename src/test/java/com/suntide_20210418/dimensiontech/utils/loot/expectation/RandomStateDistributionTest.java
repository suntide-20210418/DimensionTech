package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Map;

class RandomStateDistributionTest {
    @Test
    void equalValuesWithDifferentContinuationStatesRemainDistinct() {
        XoroshiroState1201 first = new XoroshiroState1201(1L, 2L);
        XoroshiroState1201 second = first.nextLong().state();
        RandomStateDistribution<String> distribution =
                RandomStateDistribution.of(
                        Map.of(
                                new RandomStateDistribution.State<>("same", first),
                                        ExactProbability.of(1, 2),
                                new RandomStateDistribution.State<>("same", second),
                                        ExactProbability.of(1, 2)));

        assertEquals(2, distribution.masses().size());
        assertEquals(Map.of("same", ExactProbability.ONE), distribution.marginal().masses());
    }

    @Test
    void deterministicRandomTransitionCarriesTheExactContinuation() {
        XoroshiroState1201 initial = new XoroshiroState1201(3L, 4L);
        RandomStateDistribution<Integer> result =
                RandomStateDistribution.singleton(0, initial)
                        .flatMap(
                                state -> {
                                    var draw = state.randomState().nextInt(7);
                                    return RandomStateDistribution.singleton(
                                            draw.value(), draw.state());
                                },
                                10);

        var expected = initial.nextInt(7);
        assertEquals(
                ExactProbability.ONE,
                result.masses()
                        .get(
                                new RandomStateDistribution.State<>(
                                        expected.value(), expected.state())));
    }
}
