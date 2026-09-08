package com.suntide_20210418.dimensiontech.loot.expectation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure long-run expectation calculations matching the miner's fractional accumulation rules. */
public final class ExpectationMath {
    private ExpectationMath() {}

    public static int quantityFactorHundredths(double quantity, double reference) {
        if (!Double.isFinite(reference) || reference <= 0.0D) {
            return 100;
        }
        if (!Double.isFinite(quantity) || quantity <= 0.0D) {
            return 0;
        }
        return BigDecimal.valueOf(quantity)
                .divide(BigDecimal.valueOf(reference), 2, RoundingMode.DOWN)
                .movePointRight(2)
                .min(BigDecimal.valueOf(Integer.MAX_VALUE))
                .intValue();
    }

    public static double averageParallel(
            int baseParallel, int efficiencyHundredths, int upgradeHundredths) {
        long scaled = (long) baseParallel * efficiencyHundredths * upgradeHundredths / 100L;
        return scaled / 100.0D;
    }

    public static double expectedDraws(
            double averageParallel, int drawsPerParallel, int quantityFactorHundredths) {
        return averageParallel * drawsPerParallel * quantityFactorHundredths / 100.0D;
    }

    public static double expectedItemCount(
            double itemWeight, double totalWeight, double expectedDraws) {
        if (!Double.isFinite(itemWeight)
                || itemWeight <= 0.0D
                || !Double.isFinite(totalWeight)
                || totalWeight <= 0.0D
                || !Double.isFinite(expectedDraws)
                || expectedDraws <= 0.0D) {
            return 0.0D;
        }
        return expectedDraws * itemWeight / totalWeight;
    }

    /** Converts hundredths into whole units while retaining a bounded per-slot remainder. */
    public static AccumulatedValue accumulateHundredths(
            int remainderHundredths, long scaledHundredths) {
        long nonNegativeScaled = Math.max(0L, scaledHundredths);
        int remainder = Math.max(0, Math.min(99, remainderHundredths));
        long whole = nonNegativeScaled / 100L;
        int combinedRemainder = (int) (nonNegativeScaled % 100L) + remainder;
        if (combinedRemainder >= 100) {
            whole = Math.min(Long.MAX_VALUE, whole + 1L);
            combinedRemainder -= 100;
        }
        return new AccumulatedValue(whole, combinedRemainder);
    }

    public record AccumulatedValue(long whole, int remainderHundredths) {}
}
