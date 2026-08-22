package com.suntide_20210418.dimensiontech.block.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MythicMinerExternalTickAccelerationTest {
    @Test
    void sameAccelerationUsesTheSameIndependent400TickStatisticsWindow() {
        MythicMinerExternalTickAcceleration shortCycle =
                new MythicMinerExternalTickAcceleration();
        MythicMinerExternalTickAcceleration longCycle =
                new MythicMinerExternalTickAcceleration();
        observeNaturalTicks(shortCycle, 400, 256, 400);
        observeNaturalTicks(longCycle, 40_000, 256, 400);

        assertEquals(102_400, shortCycle.currentActualTicks());
        assertEquals(shortCycle.currentActualTicks(), longCycle.currentActualTicks());
        assertEquals(1_596, shortCycle.currentExtraParallelHundredths());
        assertEquals(0, longCycle.currentExtraParallelHundredths());
    }

    @Test
    void machineCompletesImmediatelyWhenWorkTargetAnd400NaturalTicksAreReached() {
        MythicMinerExternalTickAcceleration shortCycle =
                new MythicMinerExternalTickAcceleration();
        MythicMinerExternalTickAcceleration longCycle =
                new MythicMinerExternalTickAcceleration();
        observeNaturalTicks(shortCycle, 400, 256, 399);
        observeNaturalTicks(longCycle, 40_000, 256, 399);

        MythicMinerExternalTickAcceleration.Observation shortCompletion =
                shortCycle.observe(399, 400);
        MythicMinerExternalTickAcceleration.Observation longCompletion =
                longCycle.observe(399, 40_000);

        assertTrue(shortCompletion.complete());
        assertTrue(longCompletion.complete());
        assertEquals(0, shortCompletion.actualTicks());
        assertEquals(0, longCompletion.actualTicks());
        assertEquals(0, shortCompletion.naturalTicks());
        assertEquals(0, longCompletion.naturalTicks());
    }

    @Test
    void reachingWorkTargetBefore400NaturalTicksDoesNotCompleteEarly() {
        MythicMinerExternalTickAcceleration acceleration =
                new MythicMinerExternalTickAcceleration();
        MythicMinerExternalTickAcceleration.Observation observation = null;
        for (int actualTick = 0; actualTick < 40_000; actualTick++) {
            observation = acceleration.observe(actualTick / 256L, 40_000);
        }
        assertFalse(observation.complete());
        assertEquals(157, observation.naturalTicks());
        assertEquals(40_000, observation.progressTicks());
    }

    @Test
    void acceleratedShortCycleReportsMachineTicksInsteadOfNaturalTicks() {
        MythicMinerExternalTickAcceleration acceleration =
                new MythicMinerExternalTickAcceleration();
        MythicMinerExternalTickAcceleration.Observation observation = null;
        for (int call = 0; call < 256; call++) {
            observation = acceleration.observe(0L, 400);
        }

        assertEquals(256, observation.progressTicks());
        assertEquals(256, acceleration.currentActualTicks());
        assertEquals(1, acceleration.currentCycleNaturalTicks());
        assertTrue(acceleration.currentExtraParallelHundredths() > 0);
    }

    @Test
    void naturallyLongCycleCompletesWithoutExternalParallel() {
        MythicMinerExternalTickAcceleration acceleration =
                new MythicMinerExternalTickAcceleration();
        MythicMinerExternalTickAcceleration.Observation observation = null;
        for (long gameTime = 0; gameTime < 500; gameTime++) {
            observation = acceleration.observe(gameTime, 500);
        }
        assertTrue(observation.complete());
        assertEquals(0, observation.completedExtraParallelHundredths());
        assertEquals(0, observation.actualTicks());
        assertEquals(0, observation.naturalTicks());
    }

    @Test
    void slotsKeepIndependentStatistics() {
        MythicMinerExternalTickAcceleration first =
                new MythicMinerExternalTickAcceleration();
        MythicMinerExternalTickAcceleration second =
                new MythicMinerExternalTickAcceleration();
        observeNaturalTicks(first, 500, 4, 400);
        observeNaturalTicks(second, 800, 2, 400);
        first.observe(400, 500);
        second.observe(400, 800);
        assertEquals(1_600, first.previousActualTicks());
        assertEquals(800, second.previousActualTicks());
        assertEquals(173, first.previousExtraParallelHundredths());
        assertEquals(100, second.previousExtraParallelHundredths());
    }

    @Test
    void savedStateRestoresCounters() {
        MythicMinerExternalTickAcceleration acceleration =
                new MythicMinerExternalTickAcceleration();
        observeNaturalTicks(acceleration, 40_000, 4, 200);
        MythicMinerExternalTickAcceleration restored =
                new MythicMinerExternalTickAcceleration();
        restored.load(acceleration.save());
        assertEquals(acceleration.currentActualTicks(), restored.currentActualTicks());
        assertEquals(acceleration.currentNaturalTicks(), restored.currentNaturalTicks());
        assertEquals(acceleration.currentExtraParallelHundredths(), restored.currentExtraParallelHundredths());
    }

    private static MythicMinerExternalTickAcceleration.Observation observeNaturalTicks(
            MythicMinerExternalTickAcceleration acceleration,
            int cycleTicks,
            int callsPerNaturalTick,
            int naturalTicks) {
        MythicMinerExternalTickAcceleration.Observation observation = null;
        for (long gameTime = 0; gameTime < naturalTicks; gameTime++) {
            for (int call = 0; call < callsPerNaturalTick; call++) {
                observation = acceleration.observe(gameTime, cycleTicks);
            }
        }
        return observation;
    }
}
