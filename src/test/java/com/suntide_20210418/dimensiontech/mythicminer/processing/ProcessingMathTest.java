package com.suntide_20210418.dimensiontech.mythicminer.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProcessingMathTest {
    @Test
    void processingPlanUsesTheNaturalWindowForShortWork() {
        ProcessingMath.ProcessingPlan plan =
                ProcessingMath.processingPlan(100.0D, 1.0D, 0, 400, 1);

        assertEquals(400, plan.processingTicks());
        assertEquals(400, plan.parallelHundredths());
    }

    @Test
    void processingPlanUsesCeilingForLongWorkAndScriptOverrideWins() {
        ProcessingMath.ProcessingPlan calculated =
                ProcessingMath.processingPlan(450.1D, 1.0D, 0, 400, 1);
        ProcessingMath.ProcessingPlan configured =
                ProcessingMath.processingPlan(100.0D, 1.0D, 600, 400, 1);

        assertEquals(451, calculated.processingTicks());
        assertEquals(100, calculated.parallelHundredths());
        assertEquals(600, configured.processingTicks());
        assertEquals(100, configured.parallelHundredths());
    }

    @Test
    void configuredProcessingTimeCannotUndercutTheNaturalWindow() {
        assertEquals(
                400,
                ProcessingMath.processingPlan(100.0D, 1.0D, 399, 400, 1)
                        .processingTicks());
        assertEquals(
                400,
                ProcessingMath.processingPlan(100.0D, 1.0D, 400, 400, 1)
                        .processingTicks());
        assertEquals(
                401,
                ProcessingMath.processingPlan(100.0D, 1.0D, 401, 400, 1)
                        .processingTicks());
    }

    @Test
    void processingPlanFallsBackForInvalidValues() {
        ProcessingMath.ProcessingPlan plan =
                ProcessingMath.processingPlan(Double.NaN, 0.0D, 0, 400, 1);

        assertEquals(400, plan.processingTicks());
        assertEquals(100, plan.parallelHundredths());
    }

    @Test
    void luckBelowOneUsesOnePointPerHundredPercent() {
        assertEquals(1.5F, ProcessingMath.effectiveLuck(0.5F, 100.0D));
        assertEquals(2.0F, ProcessingMath.effectiveLuck(0.0F, 200.0D));
    }

    @Test
    void luckAtOrAboveOneUsesMachineLuckAsPercentageBase() {
        assertEquals(2.0F, ProcessingMath.effectiveLuck(1.0F, 100.0D));
        assertEquals(6.0F, ProcessingMath.effectiveLuck(4.0F, 50.0D));
        assertEquals(20.0F, ProcessingMath.effectiveLuck(8.0F, 150.0D));
    }

    @Test
    void multipleUpgradePercentagesRemainAdditiveBeforeScaling() {
        assertEquals(8.0F, ProcessingMath.effectiveLuck(2.0F, 300.0D));
    }

    @Test
    void separatesUpgradedBaseParallelFromEfficiencyParallel() {
        assertEquals(192, ProcessingMath.upgradedBaseParallel(64, 300));
        assertEquals(1_536, ProcessingMath.totalParallel(64, 800, 300));
        assertEquals(1_344, ProcessingMath.extraEfficiencyParallel(64, 800, 300));
    }

    @Test
    void noEfficiencyOverflowProducesNoExtraParallel() {
        assertEquals(8, ProcessingMath.upgradedBaseParallel(4, 200));
        assertEquals(8, ProcessingMath.totalParallel(4, 100, 200));
        assertEquals(0, ProcessingMath.extraEfficiencyParallel(4, 100, 200));
    }
}
