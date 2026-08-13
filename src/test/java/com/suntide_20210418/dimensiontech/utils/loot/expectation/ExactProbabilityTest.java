package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExactProbabilityTest {
    @Test
    void reducesAndCombinesWithoutRounding() {
        ExactProbability value = ExactProbability.of(19, 2)
                .multiply(ExactProbability.of(12, 137))
                .multiply(ExactProbability.of(25, 1));

        assertEquals(ExactProbability.of(2850, 137), value);
        assertEquals(20.802919708029197D, value.doubleValue());
    }

    @Test
    void finiteDistributionRequiresAnExactUnitMass() {
        Map<String, ExactProbability> masses = new LinkedHashMap<>();
        masses.put("a", ExactProbability.of(1, 3));
        masses.put("b", ExactProbability.of(2, 3));

        FiniteDistribution<String> distribution = FiniteDistribution.of(masses);
        assertEquals(ExactProbability.ONE, distribution.masses().values().stream()
                .reduce(ExactProbability.ZERO, ExactProbability::add));
        assertThrows(IllegalArgumentException.class, () -> FiniteDistribution.of(
                Map.of("a", ExactProbability.of(1, 3), "b", ExactProbability.of(1, 3))));
    }
}
