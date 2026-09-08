package com.suntide_20210418.dimensiontech.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MythicMinerExpectationMathTest {
    @Test
    void hundredthsAccumulatorCarriesWholeValuesAndKeepsRemainder() {
        ExpectationMath.AccumulatedValue value =
                ExpectationMath.accumulateHundredths(75, 250L);

        assertEquals(3L, value.whole());
        assertEquals(25, value.remainderHundredths());
    }

    @Test
    void hundredthsAccumulatorSaturatesWholeValues() {
        ExpectationMath.AccumulatedValue value =
                ExpectationMath.accumulateHundredths(99, Long.MAX_VALUE);

        assertEquals(Long.MAX_VALUE / 100L + 1L, value.whole());
        assertEquals(6, value.remainderHundredths());
    }

    @Test
    void effectiveExpectationMatchesFractionalMachineOutput() {
        int quantityFactor = ExpectationMath.quantityFactorHundredths(5.0D, 10.0D);
        double parallel = ExpectationMath.averageParallel(3, 125, 150);
        double draws = ExpectationMath.expectedDraws(parallel, 8, quantityFactor);

        assertEquals(50, quantityFactor);
        assertEquals(5.62D, parallel, 0.000_001D);
        assertEquals(22.48D, draws, 0.000_001D);
        assertEquals(
                8.992D,
                ExpectationMath.expectedItemCount(2.0D, 5.0D, draws),
                0.000_001D);
        assertEquals(
                13.488D,
                ExpectationMath.expectedItemCount(3.0D, 5.0D, draws),
                0.000_001D);
    }

    @Test
    void quantityFactorUsesTheSameHundredthTruncationAsProduction() {
        assertEquals(33, ExpectationMath.quantityFactorHundredths(1.0D, 3.0D));
        assertEquals(100, ExpectationMath.quantityFactorHundredths(1.0D, 0.0D));
        assertEquals(0, ExpectationMath.quantityFactorHundredths(0.0D, 3.0D));
    }
}
