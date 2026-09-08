package com.suntide_20210418.dimensiontech.mythicminer.processing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure processing and attribute calculations shared by miner controllers. */
public final class ProcessingMath {
    private ProcessingMath() {}

    public static float effectiveLuck(float machineLuck, double luckIncreasePercent) {
        double percentageBase = machineLuck < 1.0F ? 1.0D : machineLuck;
        return (float) (machineLuck + percentageBase * luckIncreasePercent / 100.0D);
    }

    public static int totalParallel(
            int machineBaseParallel,
            int efficiencyParallelHundredths,
            int upgradeMultiplierHundredths) {
        long scaled =
                (long) machineBaseParallel
                        * efficiencyParallelHundredths
                        * upgradeMultiplierHundredths;
        return Math.max(1, (int) Math.min(Integer.MAX_VALUE, scaled / 10_000L));
    }

    public static int upgradedBaseParallel(int machineBaseParallel, int upgradeMultiplierHundredths) {
        return totalParallel(machineBaseParallel, 100, upgradeMultiplierHundredths);
    }

    public static int extraEfficiencyParallel(
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

    /**
     * Computes the immutable processing plan for a marker value before mutable slot state applies
     * it.
     */
    public static ProcessingPlan processingPlan(
            double structureValue,
            double efficiency,
            int configuredProcessingTime,
            int minimumNaturalTicks,
            int baseParallel) {
        int defaultTicks = Math.max(1, minimumNaturalTicks);
        int configuredTicks =
                configuredProcessingTime > 0 ? Math.max(defaultTicks, configuredProcessingTime) : 0;
        if (!Double.isFinite(efficiency)
                || efficiency <= 0.0D
                || !Double.isFinite(structureValue)
                || structureValue <= 0.0D) {
            return configuredOrDefault(configuredTicks, defaultTicks);
        }

        double calculatedTicks = structureValue / efficiency;
        if (!Double.isFinite(calculatedTicks)) {
            return new ProcessingPlan(
                    configuredTicks > 0 ? configuredTicks : Integer.MAX_VALUE, 100);
        }
        if (calculatedTicks >= defaultTicks) {
            int ticks = (int) Math.min(Integer.MAX_VALUE, Math.ceil(calculatedTicks));
            return configuredTicks > 0
                    ? new ProcessingPlan(configuredTicks, 100)
                    : new ProcessingPlan(ticks, 100);
        }

        int maxParallelHundredths = Integer.MAX_VALUE / Math.max(1, baseParallel);
        int parallelHundredths =
                BigDecimal.valueOf(defaultTicks)
                        .divide(BigDecimal.valueOf(calculatedTicks), 2, RoundingMode.DOWN)
                        .movePointRight(2)
                        .min(BigDecimal.valueOf(maxParallelHundredths))
                        .intValue();
        if (configuredTicks > 0) return new ProcessingPlan(configuredTicks, 100);
        return new ProcessingPlan(defaultTicks, Math.max(100, parallelHundredths));
    }

    private static ProcessingPlan configuredOrDefault(int configuredTicks, int defaultTicks) {
        return new ProcessingPlan(configuredTicks > 0 ? configuredTicks : defaultTicks, 100);
    }

    public record ProcessingPlan(int processingTicks, int parallelHundredths) {}
}
