package com.suntide_20210418.dimensiontech.structurereactor;

/** Integer fixed-point final settlement formulas. Percentages use basis points (10,000 = 100%). */
public final class ReactorFormula {
    public static final int BASIS_POINTS = 10_000;

    /** Settlement time before any reward or penalty is applied, in ticks. */
    public static final int NATURAL_TIME_TICKS = 400;

    public static final int MIN_TIME_TICKS = 100;
    public static final int MAX_TIME_TICKS = 800;

    /** The fluid cost never drops below a quarter or grows past twice the base cost. */
    public static final int MIN_FLUID_BP = 2_500;

    public static final int MAX_FLUID_BP = 20_000;

    /** The output never drops below half or grows past four times the target amount. */
    public static final int MIN_OUTPUT_BP = 5_000;

    public static final int MAX_OUTPUT_BP = 40_000;

    private ReactorFormula() {}

    public static Result calculate(
            int baseFluid,
            int targetOutput,
            int timeReduction,
            int timePenalty,
            int fluidReductionBp,
            int fluidPenaltyBp,
            int outputBonusBp,
            int outputPenaltyBp) {
        int time =
                clamp(
                        NATURAL_TIME_TICKS - timeReduction + timePenalty,
                        MIN_TIME_TICKS,
                        MAX_TIME_TICKS);
        long fluidNumerator =
                (long) baseFluid
                        * (BASIS_POINTS - fluidReductionBp)
                        * (BASIS_POINTS + fluidPenaltyBp);
        int fluid = ceilDiv(fluidNumerator, (long) BASIS_POINTS * BASIS_POINTS);
        fluid = clamp(fluid, minFluid(baseFluid), maxFluid(baseFluid));
        long outputNumerator =
                (long) targetOutput
                        * (BASIS_POINTS + outputBonusBp)
                        * Math.max(0, BASIS_POINTS - outputPenaltyBp);
        int output =
                (int)
                        Math.min(
                                Integer.MAX_VALUE,
                                outputNumerator / ((long) BASIS_POINTS * BASIS_POINTS));
        output = clamp(output, minOutput(targetOutput), maxOutput(targetOutput));
        return new Result(time, fluid, output);
    }

    /**
     * The closed interval a single settlement can land in, for the given recipe costs. Every field
     * is independent, so a display layer can show the reachable range without simulating a cycle.
     */
    public static Bounds bounds(int baseFluid, int targetOutput) {
        return new Bounds(
                new Result(MIN_TIME_TICKS, minFluid(baseFluid), minOutput(targetOutput)),
                new Result(MAX_TIME_TICKS, maxFluid(baseFluid), maxOutput(targetOutput)));
    }

    private static int minFluid(int baseFluid) {
        return ceilDiv((long) baseFluid * MIN_FLUID_BP, BASIS_POINTS);
    }

    /**
     * Basis points as whole percentage points, for display layers. Every reward and penalty is a
     * multiple of one percent, so the truncation never hides a fraction.
     */
    public static int percent(int basisPoints) {
        return basisPoints / (BASIS_POINTS / 100);
    }

    private static int maxFluid(int baseFluid) {
        return ceilDiv((long) baseFluid * MAX_FLUID_BP, BASIS_POINTS);
    }

    private static int minOutput(int targetOutput) {
        return (int) ((long) targetOutput * MIN_OUTPUT_BP / BASIS_POINTS);
    }

    private static int maxOutput(int targetOutput) {
        return (int) ((long) targetOutput * MAX_OUTPUT_BP / BASIS_POINTS);
    }

    private static int ceilDiv(long value, long divisor) {
        if (value <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (value + divisor - 1) / divisor);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Result(int timeTicks, int fluidCostMb, int outputAmountMb) {}

    /** The inclusive settlement range; {@code min} and {@code max} each carry all three axes. */
    public record Bounds(Result min, Result max) {}
}
