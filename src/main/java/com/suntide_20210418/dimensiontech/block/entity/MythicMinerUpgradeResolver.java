package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.block.MythicMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.MythicMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Resolves the complete upgrade state for one miner multiblock. */
final class MythicMinerUpgradeResolver {
    private MythicMinerUpgradeResolver() {}

    static Bonuses resolve(ServerLevel level, BlockPos minerPosition) {
        double efficiencyPercent = 0.0D;
        double capacityPercent = 0.0D;
        double parallelPercent = 0.0D;
        double luckPercent = 0.0D;
        double additiveConsumptionReductionPercent = 0.0D;
        double consumptionMultiplier = 1.0D;
        int efficiencyUpgradeCount = 0;
        int energyUpgradeCount = 0;
        int parallelUpgradeCount = 0;
        int luckUpgradeCount = 0;
        int aggregateUpgradeCount = 0;
        int[] upgradeCountsByTypeAndTier =
                new int[MythicMinerUpgradeBlock.Type.values().length * 6];
        for (MythicMinerUpgradeBlock block : MythicMinerMultiblock.upgrades(level, minerPosition)) {
            if (block.getType() != MythicMinerUpgradeBlock.Type.NONE) {
                upgradeCountsByTypeAndTier[(block.getType().ordinal() - 1) * 6 + block.getTier() - 1]++;
            }
            switch (block.getType()) {
                case EFFICIENCY -> {
                    efficiencyUpgradeCount++;
                    efficiencyPercent += ModConfigs.UPGRADE_TIERS[block.getTier() - 1]
                            .efficiencyIncreasePercent();
                }
                case ENERGY -> {
                    energyUpgradeCount++;
                    ModConfigs.MythicMinerUpgradeTierConfig config =
                            ModConfigs.UPGRADE_TIERS[block.getTier() - 1];
                    capacityPercent += config.energyCapacityIncreasePercent();
                    consumptionMultiplier *= 1.0D - config.energyConsumptionReductionPercent() / 100.0D;
                }
                case PARALLEL -> {
                    parallelUpgradeCount++;
                    parallelPercent += ModConfigs.UPGRADE_TIERS[block.getTier() - 1]
                            .parallelIncreasePercent();
                }
                case LUCK -> {
                    luckUpgradeCount++;
                    luckPercent += ModConfigs.UPGRADE_TIERS[block.getTier() - 1]
                            .luckIncreasePercent();
                }
                case AGGREGATE -> {
                    aggregateUpgradeCount++;
                    ModConfigs.MythicMinerUpgradeTierConfig config =
                            ModConfigs.AGGREGATE_UPGRADE_TIERS[block.getTier() - 1];
                    efficiencyPercent += config.efficiencyIncreasePercent();
                    capacityPercent += config.energyCapacityIncreasePercent();
                    additiveConsumptionReductionPercent += config.energyConsumptionReductionPercent();
                    parallelPercent += config.parallelIncreasePercent();
                    luckPercent += config.luckIncreasePercent();
                }
                case NONE -> {}
            }
        }
        consumptionMultiplier *= Math.max(0.0D, 1.0D - additiveConsumptionReductionPercent / 100.0D);
        int parallelMultiplierHundredths =
                (int) Math.min(Integer.MAX_VALUE, Math.round(100.0D + parallelPercent));
        return new Bonuses(
                1.0D + efficiencyPercent / 100.0D,
                1.0D + capacityPercent / 100.0D,
                Math.max(100, parallelMultiplierHundredths),
                luckPercent,
                Math.max(0.0D, consumptionMultiplier),
                efficiencyUpgradeCount,
                energyUpgradeCount,
                parallelUpgradeCount,
                luckUpgradeCount,
                aggregateUpgradeCount,
                upgradeCountsByTypeAndTier);
    }

    record Bonuses(
            double efficiencyMultiplier,
            double energyCapacityMultiplier,
            int parallelMultiplierHundredths,
            double luckIncreasePercent,
            double energyConsumptionMultiplier,
            int efficiencyUpgradeCount,
            int energyUpgradeCount,
            int parallelUpgradeCount,
            int luckUpgradeCount,
            int aggregateUpgradeCount,
            int[] upgradeCountsByTypeAndTier) {
        static final Bonuses NONE =
                new Bonuses(1.0D, 1.0D, 100, 0.0D, 1.0D, 0, 0, 0, 0, 0,
                        new int[MythicMinerUpgradeBlock.Type.values().length * 6]);

        int countFor(MythicMinerUpgradeBlock.Type type, int tier) {
            int index = (type.ordinal() - 1) * 6 + tier - 1;
            return index >= 0 && index < upgradeCountsByTypeAndTier.length
                    ? upgradeCountsByTypeAndTier[index]
                    : 0;
        }
    }
}
