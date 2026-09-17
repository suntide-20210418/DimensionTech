package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import org.junit.jupiter.api.Test;

class MythicCrucibleStatusDisplayTest {
    @Test
    void sequenceColorsTrackCompletedCurrentAndFutureSteps() {
        int running = MythicCrucibleCycle.Status.RUNNING.ordinal();
        assertEquals(MythicCrucibleScreen.SEQUENCE_PREVIOUS_COLOR,
                MythicCrucibleScreen.sequenceColor(0, 2, running));
        assertEquals(MythicCrucibleScreen.SEQUENCE_CURRENT_COLOR,
                MythicCrucibleScreen.sequenceColor(2, 2, running));
        assertEquals(MythicCrucibleScreen.SEQUENCE_NEXT_COLOR,
                MythicCrucibleScreen.sequenceColor(3, 2, running));
    }

    @Test
    void nonRunningSequenceHasNoCurrentStep() {
        assertEquals(MythicCrucibleScreen.SEQUENCE_PREVIOUS_COLOR,
                MythicCrucibleScreen.sequenceColor(0, -1,
                        MythicCrucibleCycle.Status.REFINING.ordinal()));
    }

    @Test
    void progressColorsUseInclusiveRewardWindowAndTimeoutPriority() {
        MythicCrucibleCycle.Status running = MythicCrucibleCycle.Status.RUNNING;
        assertEquals(MythicCrucibleScreen.SEQUENCE_NEXT_COLOR,
                MythicCrucibleScreen.progressColor(running, 19));
        assertEquals(MythicCrucibleScreen.PROGRESS_REWARD_COLOR,
                MythicCrucibleScreen.progressColor(running, 20));
        assertEquals(MythicCrucibleScreen.PROGRESS_REWARD_COLOR,
                MythicCrucibleScreen.progressColor(running, 60));
        assertEquals(MythicCrucibleScreen.SEQUENCE_NEXT_COLOR,
                MythicCrucibleScreen.progressColor(running, 61));
        assertEquals(MythicCrucibleScreen.PROGRESS_TIMEOUT_COLOR,
                MythicCrucibleScreen.progressColor(running, 100));
        assertEquals(MythicCrucibleScreen.PROGRESS_TIMEOUT_COLOR,
                MythicCrucibleScreen.progressColor(running, 120));
    }
}
