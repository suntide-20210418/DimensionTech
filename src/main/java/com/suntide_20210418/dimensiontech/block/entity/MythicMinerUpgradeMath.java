package com.suntide_20210418.dimensiontech.block.entity;

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
}
