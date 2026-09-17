package com.suntide_20210418.dimensiontech.client.gui.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle;
import org.junit.jupiter.api.Test;

class StructureReactorStatusDisplayTest {
    @Test
    void sequenceColorsTrackCompletedCurrentAndFutureSteps() {
        int running = StructureReactorCycle.Status.RUNNING.ordinal();
        assertEquals(StructureReactorScreen.SEQUENCE_PREVIOUS_COLOR,
                StructureReactorScreen.sequenceColor(0, 2, running));
        assertEquals(StructureReactorScreen.SEQUENCE_CURRENT_COLOR,
                StructureReactorScreen.sequenceColor(2, 2, running));
        assertEquals(StructureReactorScreen.SEQUENCE_NEXT_COLOR,
                StructureReactorScreen.sequenceColor(3, 2, running));
    }

    @Test
    void nonRunningSequenceHasNoCurrentStep() {
        assertEquals(StructureReactorScreen.SEQUENCE_PREVIOUS_COLOR,
                StructureReactorScreen.sequenceColor(0, -1,
                        StructureReactorCycle.Status.REFINING.ordinal()));
    }

    @Test
    void progressColorsUseInclusiveRewardWindowAndTimeoutPriority() {
        StructureReactorCycle.Status running = StructureReactorCycle.Status.RUNNING;
        assertEquals(StructureReactorScreen.SEQUENCE_NEXT_COLOR,
                StructureReactorScreen.progressColor(running, 19));
        assertEquals(StructureReactorScreen.PROGRESS_REWARD_COLOR,
                StructureReactorScreen.progressColor(running, 20));
        assertEquals(StructureReactorScreen.PROGRESS_REWARD_COLOR,
                StructureReactorScreen.progressColor(running, 60));
        assertEquals(StructureReactorScreen.SEQUENCE_NEXT_COLOR,
                StructureReactorScreen.progressColor(running, 61));
        assertEquals(StructureReactorScreen.PROGRESS_TIMEOUT_COLOR,
                StructureReactorScreen.progressColor(running, 100));
        assertEquals(StructureReactorScreen.PROGRESS_TIMEOUT_COLOR,
                StructureReactorScreen.progressColor(running, 120));
    }
}
