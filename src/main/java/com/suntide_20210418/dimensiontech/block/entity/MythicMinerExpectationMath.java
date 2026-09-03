package com.suntide_20210418.dimensiontech.block.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure long-run expectation calculations matching the miner's fractional accumulation rules. */
final class MythicMinerExpectationMath {
    private MythicMinerExpectationMath() {}

    static int quantityFactorHundredths(double quantity, double reference) {
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

    static double averageParallel(
            int baseParallel, int efficiencyHundredths, int upgradeHundredths) {
        long scaled = (long) baseParallel * efficiencyHundredths * upgradeHundredths / 100L;
        return scaled / 100.0D;
    }

    static double expectedDraws(
            double averageParallel, int drawsPerParallel, int quantityFactorHundredths) {
        return averageParallel * drawsPerParallel * quantityFactorHundredths / 100.0D;
    }

    static double expectedItemCount(double itemWeight, double totalWeight, double expectedDraws) {
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
}
