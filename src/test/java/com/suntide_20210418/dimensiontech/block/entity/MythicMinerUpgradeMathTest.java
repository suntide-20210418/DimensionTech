package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MythicMinerUpgradeMathTest {
    @Test
    void processingPlanUsesTheNaturalWindowForShortWork() {
        MythicMinerUpgradeMath.ProcessingPlan plan =
                MythicMinerUpgradeMath.processingPlan(100.0D, 1.0D, 0, 400, 1);

        assertEquals(400, plan.processingTicks());
        assertEquals(400, plan.parallelHundredths());
    }

    @Test
    void processingPlanUsesCeilingForLongWorkAndScriptOverrideWins() {
        MythicMinerUpgradeMath.ProcessingPlan calculated =
                MythicMinerUpgradeMath.processingPlan(450.1D, 1.0D, 0, 400, 1);
        MythicMinerUpgradeMath.ProcessingPlan configured =
                MythicMinerUpgradeMath.processingPlan(100.0D, 1.0D, 275, 400, 1);

        assertEquals(451, calculated.processingTicks());
        assertEquals(100, calculated.parallelHundredths());
        assertEquals(275, configured.processingTicks());
        assertEquals(100, configured.parallelHundredths());
    }

    @Test
    void processingPlanFallsBackForInvalidValues() {
        MythicMinerUpgradeMath.ProcessingPlan plan =
                MythicMinerUpgradeMath.processingPlan(Double.NaN, 0.0D, 0, 400, 1);

        assertEquals(400, plan.processingTicks());
        assertEquals(100, plan.parallelHundredths());
    }

    @Test
    void luckBelowOneUsesOnePointPerHundredPercent() {
        assertEquals(1.5F, MythicMinerUpgradeMath.effectiveLuck(0.5F, 100.0D));
        assertEquals(2.0F, MythicMinerUpgradeMath.effectiveLuck(0.0F, 200.0D));
    }

    @Test
    void luckAtOrAboveOneUsesMachineLuckAsPercentageBase() {
        assertEquals(2.0F, MythicMinerUpgradeMath.effectiveLuck(1.0F, 100.0D));
        assertEquals(6.0F, MythicMinerUpgradeMath.effectiveLuck(4.0F, 50.0D));
        assertEquals(20.0F, MythicMinerUpgradeMath.effectiveLuck(8.0F, 150.0D));
    }

    @Test
    void multipleUpgradePercentagesRemainAdditiveBeforeScaling() {
        assertEquals(8.0F, MythicMinerUpgradeMath.effectiveLuck(2.0F, 300.0D));
    }

    @Test
    void separatesUpgradedBaseParallelFromEfficiencyParallel() {
        assertEquals(192, MythicMinerUpgradeMath.upgradedBaseParallel(64, 300));
        assertEquals(1_536, MythicMinerUpgradeMath.totalParallel(64, 800, 300));
        assertEquals(1_344, MythicMinerUpgradeMath.extraEfficiencyParallel(64, 800, 300));
    }

    @Test
    void noEfficiencyOverflowProducesNoExtraParallel() {
        assertEquals(8, MythicMinerUpgradeMath.upgradedBaseParallel(4, 200));
        assertEquals(8, MythicMinerUpgradeMath.totalParallel(4, 100, 200));
        assertEquals(0, MythicMinerUpgradeMath.extraEfficiencyParallel(4, 100, 200));
    }
}
