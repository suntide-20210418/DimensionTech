package com.suntide_20210418.dimensiontech.structurereactor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReactorFormulaTest {
    @Test
    void clampsTimeFluidAndOutputToV1Bounds() {
        ReactorFormula.Result minimum = ReactorFormula.calculate(
                1_000, 1_000, 10_000, 0, 10_000, 0, 0, 0);
        assertEquals(100, minimum.timeTicks());
        assertEquals(250, minimum.fluidCostMb());
        assertEquals(1_000, minimum.outputAmountMb());

        ReactorFormula.Result maximum = ReactorFormula.calculate(
                1_000, 1_000, 0, 10_000, 0, 10_000, 30_000, 0);
        assertEquals(800, maximum.timeTicks());
        assertEquals(2_000, maximum.fluidCostMb());
        assertEquals(4_000, maximum.outputAmountMb());
    }

    @Test
    void penaltiesParticipateInFinalFormula() {
        ReactorFormula.Result result = ReactorFormula.calculate(
                1_000, 1_000, 0, 80, 0, 2_500, 0, 1_000);
        assertEquals(480, result.timeTicks());
        assertEquals(1_250, result.fluidCostMb());
        assertEquals(900, result.outputAmountMb());
    }

    /** Display layers print whole percentage points; every rule value is a multiple of one. */
    @Test
    void basisPointsConvertToWholePercentages() {
        assertEquals(10, ReactorFormula.percent(1_000));
        assertEquals(25, ReactorFormula.percent(2_500));
        assertEquals(0, ReactorFormula.percent(0));
    }
}
