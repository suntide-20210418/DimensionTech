package com.suntide_20210418.dimensiontech.structurereactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The settlement range a display layer shows must be exactly what the formula can produce, so the
 * floor and ceiling are re-derived by driving {@link ReactorFormula#calculate} to both ends.
 *
 * <p>Nothing here touches game registries: the formula is pure integer arithmetic.
 */
class ReactorFormulaBoundsTest {
    private static final int BASE_FLUID = 1_000;
    private static final int TARGET_OUTPUT = 1_000;

    /** A substitute settles every state outside the reward window, which is the worst case. */
    private static final int WORST_TIME_REDUCTION = 10_000;

    private static final int WORST_TIME_PENALTY = 10_000;
    private static final int REWARDED_OUTPUT_BONUS_BP = 30_000;

    @Test
    void boundsAreAttainableAtBothEnds() {
        ReactorFormula.Bounds bounds = ReactorFormula.bounds(BASE_FLUID, TARGET_OUTPUT);

        ReactorFormula.Result floor =
                ReactorFormula.calculate(
                        BASE_FLUID,
                        TARGET_OUTPUT,
                        WORST_TIME_REDUCTION,
                        0,
                        ReactorFormula.BASIS_POINTS,
                        0,
                        0,
                        ReactorFormula.BASIS_POINTS);
        ReactorFormula.Result ceiling =
                ReactorFormula.calculate(
                        BASE_FLUID,
                        TARGET_OUTPUT,
                        0,
                        WORST_TIME_PENALTY,
                        0,
                        ReactorFormula.BASIS_POINTS,
                        REWARDED_OUTPUT_BONUS_BP,
                        0);

        assertEquals(bounds.min(), floor, "the reported minimum must be reachable");
        assertEquals(bounds.max(), ceiling, "the reported maximum must be reachable");
    }

    @Test
    void timeFloorAndCeilingAreReachable() {
        assertEquals(
                ReactorFormula.MIN_TIME_TICKS,
                ReactorFormula.calculate(BASE_FLUID, TARGET_OUTPUT, WORST_TIME_REDUCTION, 0, 0, 0, 0, 0)
                        .timeTicks());
        assertEquals(
                ReactorFormula.MAX_TIME_TICKS,
                ReactorFormula.calculate(BASE_FLUID, TARGET_OUTPUT, 0, WORST_TIME_PENALTY, 0, 0, 0, 0)
                        .timeTicks());
        assertEquals(
                ReactorFormula.MIN_TIME_TICKS,
                ReactorFormula.bounds(BASE_FLUID, TARGET_OUTPUT).min().timeTicks());
        assertEquals(
                ReactorFormula.MAX_TIME_TICKS,
                ReactorFormula.bounds(BASE_FLUID, TARGET_OUTPUT).max().timeTicks());
    }

    @Test
    void boundsUseTheDocumentedClampConstantsForTheDefaultCosts() {
        ReactorFormula.Bounds bounds = ReactorFormula.bounds(BASE_FLUID, TARGET_OUTPUT);

        assertEquals(250, bounds.min().fluidCostMb(), "a quarter of the base cost");
        assertEquals(2_000, bounds.max().fluidCostMb(), "twice the base cost");
        assertEquals(500, bounds.min().outputAmountMb(), "half the target output");
        assertEquals(4_000, bounds.max().outputAmountMb(), "four times the target output");
    }

    @Test
    void unrewardedSettlementIsTheRecipesOwnCost() {
        ReactorFormula.Result natural =
                ReactorFormula.calculate(BASE_FLUID, TARGET_OUTPUT, 0, 0, 0, 0, 0, 0);
        ReactorFormula.Bounds bounds = ReactorFormula.bounds(BASE_FLUID, TARGET_OUTPUT);

        assertEquals(ReactorFormula.NATURAL_TIME_TICKS, natural.timeTicks());
        assertEquals(BASE_FLUID, natural.fluidCostMb());
        assertEquals(TARGET_OUTPUT, natural.outputAmountMb());
        assertTrue(natural.timeTicks() >= bounds.min().timeTicks());
        assertTrue(natural.timeTicks() <= bounds.max().timeTicks());
    }

    @Test
    void boundsStayOrderedForATinyCost() {
        ReactorFormula.Bounds bounds = ReactorFormula.bounds(1, 1);

        assertTrue(bounds.min().fluidCostMb() <= bounds.max().fluidCostMb());
        assertTrue(bounds.min().outputAmountMb() <= bounds.max().outputAmountMb());
        assertTrue(bounds.min().fluidCostMb() >= 1, "a rounded-up cost must stay positive");
    }
}
