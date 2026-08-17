package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.List;

class IdealRandomProbabilitySpace1201Test {
    @Test
    void weightedSelectionRetainsTheExactSourceLevelProbability() {
        var result = ExactRandomSemantics1201.weightedIndex(List.of(12, 125));

        assertEquals(ExactProbability.of(12, 137), result.distribution().masses().get(0));
        assertEquals(
                "minecraft_1_20_1_randomsource_ideal_finite_v1",
                IdealRandomProbabilitySpace1201.ID);
    }
}
