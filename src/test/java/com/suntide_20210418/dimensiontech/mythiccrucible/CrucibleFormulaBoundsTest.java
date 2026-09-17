package com.suntide_20210418.dimensiontech.mythiccrucible;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The settlement range a display layer shows must be exactly what the formula can produce, so the
 * floor and ceiling are re-derived by driving {@link CrucibleFormula#calculate} to both ends.
 *
 * <p>Nothing here touches game registries: the formula is pure integer arithmetic.
 */
class CrucibleFormulaBoundsTest {
    private static final int BASE_FLUID = 1_000;
    private static final int TARGET_OUTPUT = 1_000;

    /** A substitute settles every state outside the reward window, which is the worst case. */
    private static final int WORST_TIME_REDUCTION = 10_000;

    private static final int WORST_TIME_PENALTY = 10_000;
    private static final int REWARDED_OUTPUT_BONUS_BP = 30_000;

    @Test
    void boundsAreAttainableAtBothEnds() {
        CrucibleFormula.Bounds bounds = CrucibleFormula.bounds(BASE_FLUID, TARGET_OUTPUT);

        CrucibleFormula.Result floor =
                CrucibleFormula.calculate(
                        BASE_FLUID,
                        TARGET_OUTPUT,
                        WORST_TIME_REDUCTION,
                        0,
                        CrucibleFormula.BASIS_POINTS,
                        0,
                        0,
                        CrucibleFormula.BASIS_POINTS);
        CrucibleFormula.Result ceiling =
                CrucibleFormula.calculate(
                        BASE_FLUID,
                        TARGET_OUTPUT,
                        0,
                        WORST_TIME_PENALTY,
                        0,
                        CrucibleFormula.BASIS_POINTS,
                        REWARDED_OUTPUT_BONUS_BP,
                        0);

        assertEquals(bounds.min(), floor, "the reported minimum must be reachable");
        assertEquals(bounds.max(), ceiling, "the reported maximum must be reachable");
    }

    @Test
    void timeFloorAndCeilingAreReachable() {
        assertEquals(
                CrucibleFormula.MIN_TIME_TICKS,
                CrucibleFormula.calculate(BASE_FLUID, TARGET_OUTPUT, WORST_TIME_REDUCTION, 0, 0, 0, 0, 0)
                        .timeTicks());
        assertEquals(
                CrucibleFormula.MAX_TIME_TICKS,
                CrucibleFormula.calculate(BASE_FLUID, TARGET_OUTPUT, 0, WORST_TIME_PENALTY, 0, 0, 0, 0)
                        .timeTicks());
        assertEquals(
                CrucibleFormula.MIN_TIME_TICKS,
                CrucibleFormula.bounds(BASE_FLUID, TARGET_OUTPUT).min().timeTicks());
        assertEquals(
                CrucibleFormula.MAX_TIME_TICKS,
                CrucibleFormula.bounds(BASE_FLUID, TARGET_OUTPUT).max().timeTicks());
    }

    @Test
    void boundsUseTheDocumentedClampConstantsForTheDefaultCosts() {
        CrucibleFormula.Bounds bounds = CrucibleFormula.bounds(BASE_FLUID, TARGET_OUTPUT);

        assertEquals(250, bounds.min().fluidCostMb(), "a quarter of the base cost");
        assertEquals(2_000, bounds.max().fluidCostMb(), "twice the base cost");
        assertEquals(500, bounds.min().outputAmountMb(), "half the target output");
        assertEquals(4_000, bounds.max().outputAmountMb(), "four times the target output");
    }

    @Test
    void unrewardedSettlementIsTheRecipesOwnCost() {
        CrucibleFormula.Result natural =
                CrucibleFormula.calculate(BASE_FLUID, TARGET_OUTPUT, 0, 0, 0, 0, 0, 0);
        CrucibleFormula.Bounds bounds = CrucibleFormula.bounds(BASE_FLUID, TARGET_OUTPUT);

        assertEquals(CrucibleFormula.NATURAL_TIME_TICKS, natural.timeTicks());
        assertEquals(BASE_FLUID, natural.fluidCostMb());
        assertEquals(TARGET_OUTPUT, natural.outputAmountMb());
        assertTrue(natural.timeTicks() >= bounds.min().timeTicks());
        assertTrue(natural.timeTicks() <= bounds.max().timeTicks());
    }

    @Test
    void boundsStayOrderedForATinyCost() {
        CrucibleFormula.Bounds bounds = CrucibleFormula.bounds(1, 1);

        assertTrue(bounds.min().fluidCostMb() <= bounds.max().fluidCostMb());
        assertTrue(bounds.min().outputAmountMb() <= bounds.max().outputAmountMb());
        assertTrue(bounds.min().fluidCostMb() >= 1, "a rounded-up cost must stay positive");
    }
}
