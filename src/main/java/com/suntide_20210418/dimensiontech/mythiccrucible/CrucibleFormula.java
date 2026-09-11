package com.suntide_20210418.dimensiontech.mythiccrucible;

/** Integer fixed-point final settlement formulas. Percentages use basis points (10,000 = 100%). */
public final class CrucibleFormula {
    public static final int BASIS_POINTS = 10_000;

    private CrucibleFormula() {}

    public static Result calculate(
            int baseFluid,
            int targetOutput,
            int timeReduction,
            int timePenalty,
            int fluidReductionBp,
            int fluidPenaltyBp,
            int outputBonusBp,
            int outputPenaltyBp) {
        int time = clamp(400 - timeReduction + timePenalty, 100, 800);
        long fluidNumerator =
                (long) baseFluid
                        * (BASIS_POINTS - fluidReductionBp)
                        * (BASIS_POINTS + fluidPenaltyBp);
        int fluid = ceilDiv(fluidNumerator, (long) BASIS_POINTS * BASIS_POINTS);
        fluid =
                clamp(
                        fluid,
                        ceilDiv((long) baseFluid * 2_500, BASIS_POINTS),
                        ceilDiv((long) baseFluid * 20_000, BASIS_POINTS));
        long outputNumerator =
                (long) targetOutput
                        * (BASIS_POINTS + outputBonusBp)
                        * Math.max(0, BASIS_POINTS - outputPenaltyBp);
        int output =
                (int)
                        Math.min(
                                Integer.MAX_VALUE,
                                outputNumerator / ((long) BASIS_POINTS * BASIS_POINTS));
        output =
                clamp(
                        output,
                        (int) ((long) targetOutput * 5_000 / BASIS_POINTS),
                        (int) ((long) targetOutput * 40_000 / BASIS_POINTS));
        return new Result(time, fluid, output);
    }

    private static int ceilDiv(long value, long divisor) {
        if (value <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (value + divisor - 1) / divisor);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Result(int timeTicks, int fluidCostMb, int outputAmountMb) {}
}
