package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.block.MythicMinerMultiblock;
import com.suntide_20210418.dimensiontech.block.MythicMinerUpgradeBlock;
import com.suntide_20210418.dimensiontech.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Owns multiblock upgrade discovery and exposes one immutable bonus snapshot per refresh. */
final class MinerUpgradeController {
    private final BlockPos minerPosition;
    private UpgradeState state = UpgradeState.NONE;

    MinerUpgradeController(BlockPos minerPosition) {
        this.minerPosition = minerPosition;
    }

    UpgradeState refresh(ServerLevel level) {
        state = UpgradeState.from(level, minerPosition);
        return state;
    }

    UpgradeState state() {
        return state;
    }

    record UpgradeState(
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
        static final UpgradeState NONE =
                new UpgradeState(
                        1.0D,
                        1.0D,
                        100,
                        0.0D,
                        1.0D,
                        0,
                        0,
                        0,
                        0,
                        0,
                        new int[MythicMinerUpgradeBlock.Type.values().length * 6]);

        private static UpgradeState from(ServerLevel level, BlockPos position) {
            double efficiency = 0.0D;
            double capacity = 0.0D;
            double parallel = 0.0D;
            double luck = 0.0D;
            double additiveReduction = 0.0D;
            double consumption = 1.0D;
            int efficiencyCount = 0;
            int energyCount = 0;
            int parallelCount = 0;
            int luckCount = 0;
            int aggregateCount = 0;
            int[] counts = new int[MythicMinerUpgradeBlock.Type.values().length * 6];
            for (MythicMinerUpgradeBlock block : MythicMinerMultiblock.upgrades(level, position)) {
                if (block.getType() != MythicMinerUpgradeBlock.Type.NONE) {
                    counts[(block.getType().ordinal() - 1) * 6 + block.getTier() - 1]++;
                }
                switch (block.getType()) {
                    case EFFICIENCY -> {
                        efficiencyCount++;
                        efficiency +=
                                ModConfigs.UPGRADE_TIERS[block.getTier() - 1]
                                        .efficiencyIncreasePercent();
                    }
                    case ENERGY -> {
                        energyCount++;
                        var config = ModConfigs.UPGRADE_TIERS[block.getTier() - 1];
                        capacity += config.energyCapacityIncreasePercent();
                        consumption *= 1.0D - config.energyConsumptionReductionPercent() / 100.0D;
                    }
                    case PARALLEL -> {
                        parallelCount++;
                        parallel +=
                                ModConfigs.UPGRADE_TIERS[block.getTier() - 1]
                                        .parallelIncreasePercent();
                    }
                    case LUCK -> {
                        luckCount++;
                        luck += ModConfigs.UPGRADE_TIERS[block.getTier() - 1].luckIncreasePercent();
                    }
                    case AGGREGATE -> {
                        aggregateCount++;
                        var config = ModConfigs.AGGREGATE_UPGRADE_TIERS[block.getTier() - 1];
                        efficiency += config.efficiencyIncreasePercent();
                        capacity += config.energyCapacityIncreasePercent();
                        additiveReduction += config.energyConsumptionReductionPercent();
                        parallel += config.parallelIncreasePercent();
                        luck += config.luckIncreasePercent();
                    }
                    case NONE -> {}
                }
            }
            consumption *= Math.max(0.0D, 1.0D - additiveReduction / 100.0D);
            return new UpgradeState(
                    1.0D + efficiency / 100.0D,
                    1.0D + capacity / 100.0D,
                    (int) Math.min(Integer.MAX_VALUE, Math.round(100.0D + parallel)),
                    luck,
                    Math.max(0.0D, consumption),
                    efficiencyCount,
                    energyCount,
                    parallelCount,
                    luckCount,
                    aggregateCount,
                    counts);
        }

        int countFor(MythicMinerUpgradeBlock.Type type, int tier) {
            int index = (type.ordinal() - 1) * 6 + tier - 1;
            return index >= 0 && index < upgradeCountsByTypeAndTier.length
                    ? upgradeCountsByTypeAndTier[index]
                    : 0;
        }
    }
}
