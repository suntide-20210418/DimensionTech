package com.suntide_20210418.dimensiontech.block.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure upgrade calculations shared by machine logic and unit tests. */
final class MythicMinerUpgradeMath {
    private MythicMinerUpgradeMath() {}

    static float effectiveLuck(float machineLuck, double luckIncreasePercent) {
        double percentageBase = machineLuck < 1.0F ? 1.0D : machineLuck;
        return (float) (machineLuck + percentageBase * luckIncreasePercent / 100.0D);
    }

    static int totalParallel(
            int machineBaseParallel,
            int efficiencyParallelHundredths,
            int upgradeMultiplierHundredths) {
        long scaled =
                (long) machineBaseParallel
                        * efficiencyParallelHundredths
                        * upgradeMultiplierHundredths;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, scaled / 10_000L));
    }

    static int upgradedBaseParallel(int machineBaseParallel, int upgradeMultiplierHundredths) {
        return totalParallel(machineBaseParallel, 100, upgradeMultiplierHundredths);
    }

    static int extraEfficiencyParallel(
            int machineBaseParallel,
            int efficiencyParallelHundredths,
            int upgradeMultiplierHundredths) {
        return Math.max(
                0,
                totalParallel(
                                machineBaseParallel,
                                efficiencyParallelHundredths,
                                upgradeMultiplierHundredths)
                        - upgradedBaseParallel(machineBaseParallel, upgradeMultiplierHundredths));
    }

    /** Computes the immutable processing plan for a marker value before mutable slot state applies it. */
    static ProcessingPlan processingPlan(
            double structureValue,
            double efficiency,
            int configuredProcessingTime,
            int minimumNaturalTicks,
            int baseParallel) {
        int defaultTicks = Math.max(1, minimumNaturalTicks);
        if (!Double.isFinite(efficiency)
                || efficiency <= 0.0D
                || !Double.isFinite(structureValue)
                || structureValue <= 0.0D) {
            return configuredOrDefault(configuredProcessingTime, defaultTicks);
        }

        double calculatedTicks = structureValue / efficiency;
        if (!Double.isFinite(calculatedTicks)) {
            return new ProcessingPlan(
                    configuredProcessingTime > 0 ? configuredProcessingTime : Integer.MAX_VALUE, 100);
        }
        if (calculatedTicks >= defaultTicks) {
            int ticks = (int) Math.min(Integer.MAX_VALUE, Math.ceil(calculatedTicks));
            return configuredProcessingTime > 0
                    ? new ProcessingPlan(configuredProcessingTime, 100)
                    : new ProcessingPlan(ticks, 100);
        }

        int maxParallelHundredths = Integer.MAX_VALUE / Math.max(1, baseParallel);
        int parallelHundredths =
                BigDecimal.valueOf(defaultTicks)
                        .divide(BigDecimal.valueOf(calculatedTicks), 2, RoundingMode.DOWN)
                        .movePointRight(2)
                        .min(BigDecimal.valueOf(maxParallelHundredths))
                        .intValue();
        if (configuredProcessingTime > 0) return new ProcessingPlan(configuredProcessingTime, 100);
        return new ProcessingPlan(defaultTicks, Math.max(100, parallelHundredths));
    }

    private static ProcessingPlan configuredOrDefault(int configuredProcessingTime, int defaultTicks) {
        return new ProcessingPlan(configuredProcessingTime > 0 ? configuredProcessingTime : defaultTicks, 100);
    }

    record ProcessingPlan(int processingTicks, int parallelHundredths) {}
}
