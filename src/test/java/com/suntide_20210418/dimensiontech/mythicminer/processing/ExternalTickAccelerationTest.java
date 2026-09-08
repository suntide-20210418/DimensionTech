package com.suntide_20210418.dimensiontech.mythicminer.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExternalTickAccelerationTest {
    @Test
    void equivalentAccelerationExcludesTheNaturalTickBaselineCall() {
        for (int multiplier = 2; multiplier <= 256; multiplier <<= 1) {
            ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
            for (int call = 0; call < multiplier + 1; call++) {
                acceleration.observe(0L, 40_000);
            }
            acceleration.observe(1L, 40_000);
            assertEquals(
                    multiplier,
                    acceleration.currentEquivalentAccelerationTicks(),
                    "unexpected equivalent acceleration for " + multiplier + "x");
        }
    }

    @Test
    void sameNaturalTickCountsOneLogicalTickAndEveryCallAsActualTick() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        for (int call = 0; call < 257; call++) {
            acceleration.observe(0L, 400);
        }
        assertEquals(1L, acceleration.currentNaturalTicks());
        assertEquals(257L, acceleration.currentActualTicks());
        acceleration.observe(1L, 400);
        assertEquals(256L, acceleration.currentEquivalentAccelerationTicks());
    }

    @Test
    void completionWaitsForTheNaturalWindowBoundary() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        ExternalTickAcceleration.Observation observation = null;
        for (long gameTime = 0; gameTime < 399; gameTime++) {
            observation = acceleration.observe(gameTime, 400);
        }
        assertFalse(observation.complete());
        observation = acceleration.observe(399L, 400);
        assertTrue(observation.complete());
        assertEquals(400L, observation.completedActualTicks());
    }

    @Test
    void acceleratedCycleWaitsFor400NaturalTicksAndSettlesParallel() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        ExternalTickAcceleration.Observation observation = null;
        for (long gameTime = 0; gameTime < 400; gameTime++) {
            for (int call = 0; call < 129; call++) {
                observation = acceleration.observe(gameTime, 400);
            }
        }
        assertTrue(observation.complete());
        assertEquals(0L, observation.logicalProgressTicks());
        assertEquals(0L, observation.actualProgressTicks());
        assertEquals(102_400L, observation.completedActualTicks());
        assertEquals(1_596, observation.completedParallel());
        assertEquals(102_400L, acceleration.previousActualTicks());
    }

    @Test
    void longCycleReachedAfterNaturalWindowCompletesWithoutParallel() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        ExternalTickAcceleration.Observation observation = null;
        for (long gameTime = 0; gameTime < 40_000; gameTime++) {
            observation = acceleration.observe(gameTime, 40_000);
        }
        assertTrue(observation.complete());
        assertEquals(0, observation.completedParallel());
        assertEquals(40_000L, observation.completedActualTicks());
    }

    @Test
    void acceleratedParallelUsesConfiguredCycleTicksAsDenominator() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        ExternalTickAcceleration.Observation observation = null;
        for (long gameTime = 0; gameTime < 400; gameTime++) {
            for (int call = 0; call < 129; call++) {
                observation = acceleration.observe(gameTime, 40_000);
            }
        }
        assertTrue(observation.complete());
        assertEquals(124, observation.completedParallel());
    }

    @Test
    void targetReachedExactlyAt400NaturalTicksDoesNotUseParallel() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        ExternalTickAcceleration.Observation observation = null;
        for (long gameTime = 0; gameTime < 400; gameTime++) {
            observation = acceleration.observe(gameTime, 400);
        }
        assertTrue(observation.complete());
        assertEquals(0, observation.completedParallel());
    }

    @Test
    void longActualTickSaturatesWithoutOverflow() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        acceleration.load(
                new ExternalTickAcceleration.State(
                        0L,
                        Long.MAX_VALUE - 1L,
                        1L,
                        0L,
                        0L,
                        false,
                        0,
                        0L,
                        0,
                        true));
        acceleration.observe(0L, Integer.MAX_VALUE);
        acceleration.observe(0L, Integer.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, acceleration.currentActualTicks());
    }

    @Test
    void savedStateRestoresLongCounters() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        acceleration.load(
                new ExternalTickAcceleration.State(
                        10L,
                        4_000_000_000L,
                        200L,
                        0L,
                        256L,
                        true,
                        0,
                        3_000_000_000L,
                        100,
                        true));
        ExternalTickAcceleration restored = new ExternalTickAcceleration();
        restored.load(acceleration.save());
        assertEquals(4_000_000_000L, restored.currentActualTicks());
        assertEquals(200L, restored.currentNaturalTicks());
        assertEquals(acceleration.currentExtraParallelHundredths(),
                restored.currentExtraParallelHundredths());
    }

    @Test
    void cycleDoesNotCompleteBeforeNaturalMinimum() {
        ExternalTickAcceleration acceleration = new ExternalTickAcceleration();
        ExternalTickAcceleration.Observation observation = null;
        for (int call = 0; call < 40_000; call++) {
            observation = acceleration.observe(call / 256L, 40_000);
        }
        assertFalse(observation.complete());
        assertEquals(157L, observation.logicalProgressTicks());
        assertEquals(40_000L, observation.actualProgressTicks());
    }
}
