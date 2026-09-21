package com.suntide_20210418.dimensiontech.structurereactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins the comparator interface. The sixteen levels are a redstone contract players build against,
 * so every expectation is written out literally rather than recomputed from the code under test -
 * renaming a step or moving a level has to break this file loudly.
 */
class ReactorAnalogSignalTest {
    private static final StructureReactorCycle.Status IDLE = StructureReactorCycle.Status.IDLE;
    private static final StructureReactorCycle.Status RUNNING = StructureReactorCycle.Status.RUNNING;

    @Test
    void idleSeparatesWaitingFromBlocked() {
        assertEquals(0, ReactorAnalogSignal.of(IDLE, null, 0, false));
        assertEquals(13, ReactorAnalogSignal.of(IDLE, null, 0, true));
    }

    @Test
    void eachOperationStepOwnsALevelOfItsOwn() {
        assertEquals(1, level(StateId.BRANCH, 0));
        assertEquals(2, level(StateId.RECURSE, 0));
        assertEquals(3, level(StateId.CONVERGE, 0));
        assertEquals(4, level(StateId.STABILIZE, 0));
    }

    @Test
    void theRewardWindowShiftsEveryStepByFour() {
        int opening = StructureReactorCycle.REWARD_START_TICK;
        assertEquals(5, level(StateId.BRANCH, opening));
        assertEquals(6, level(StateId.RECURSE, opening));
        assertEquals(7, level(StateId.CONVERGE, opening));
        assertEquals(8, level(StateId.STABILIZE, opening));
    }

    @Test
    void theRewardWindowIsClosedAtBothEnds() {
        int opening = StructureReactorCycle.REWARD_START_TICK;
        int closing = StructureReactorCycle.REWARD_END_TICK;
        assertFalse(ReactorAnalogSignal.inRewardWindow(opening - 1));
        assertTrue(ReactorAnalogSignal.inRewardWindow(opening));
        assertTrue(ReactorAnalogSignal.inRewardWindow(closing));
        assertFalse(ReactorAnalogSignal.inRewardWindow(closing + 1));
    }

    @Test
    void aTimedOutStepDropsBackOutOfTheWindow() {
        // Past the timeout the step settles as a penalty, so the level must not stay on the rewarded
        // half and invite a submission that cannot be rewarded.
        assertEquals(1, level(StateId.BRANCH, StructureReactorCycle.STATE_TIMEOUT_TICKS));
    }

    @Test
    void refiningAndABlockedCommitKeepTheirOwnLevels() {
        assertEquals(
                15,
                ReactorAnalogSignal.of(
                        StructureReactorCycle.Status.REFINING, StateId.BRANCH, 0, false));
        assertEquals(
                14,
                ReactorAnalogSignal.of(
                        StructureReactorCycle.Status.READY_TO_COMMIT, StateId.STABILIZE, 0, true));
    }

    @Test
    void aBlockedCommitIgnoresTheStartVerdict() {
        // The start verdict only describes an idle reactor; it must never mask a stalled commit.
        assertEquals(
                14,
                ReactorAnalogSignal.of(StructureReactorCycle.Status.READY_TO_COMMIT, null, 0, false));
    }

    @Test
    void aCircuitDecodesAStepWithoutKnowingTheRecipe() {
        int steps = StateId.values().length;
        for (int signal = 1; signal <= 2 * steps; signal++) {
            StateId step = StateId.values()[(signal - 1) % steps];
            boolean window = signal > steps;
            int ticks = window ? StructureReactorCycle.REWARD_START_TICK : 0;
            assertEquals(signal, level(step, ticks), "decoding level " + signal);
        }
    }

    @Test
    void theStepsFormAContiguousRunStartingAtOne() {
        // Surfaces advertise the operation levels as one range instead of listing every step, which
        // only holds while declaration order matches level order with no gaps.
        StateId[] steps = StateId.values();
        for (int index = 0; index < steps.length; index++) {
            assertEquals(index + 1, steps[index].signalLevel(), steps[index].toString());
        }
    }

    @Test
    void noTwoMeaningsShareALevel() {
        List<Integer> assigned = new ArrayList<>();
        for (StateId state : StateId.values()) {
            assigned.add(state.signalLevel());
            assigned.add(state.signalLevel() + ReactorAnalogSignal.REWARD_WINDOW_OFFSET);
        }
        assigned.add(ReactorAnalogSignal.IDLE);
        assigned.add(ReactorAnalogSignal.IDLE_BLOCKED);
        assigned.add(ReactorAnalogSignal.COMMIT_BLOCKED);
        assigned.add(ReactorAnalogSignal.REFINING);
        assertEquals(assigned.size(), (int) assigned.stream().distinct().count(), assigned.toString());
    }

    @Test
    void everyReachableLevelStaysInsideTheComparatorsRange() {
        for (int stateTicks = 0; stateTicks <= StructureReactorCycle.STATE_TIMEOUT_TICKS; stateTicks++) {
            for (StructureReactorCycle.Status status : StructureReactorCycle.Status.values()) {
                for (StateId state : StateId.values()) {
                    for (boolean blocked : new boolean[] {false, true}) {
                        int signal = ReactorAnalogSignal.of(status, state, stateTicks, blocked);
                        assertTrue(
                                signal >= ReactorAnalogSignal.MIN_VALUE, "below range: " + signal);
                        assertTrue(
                                signal <= ReactorAnalogSignal.MAX_VALUE, "above range: " + signal);
                    }
                }
            }
        }
    }

    private static int level(StateId state, int stateTicks) {
        return ReactorAnalogSignal.of(RUNNING, state, stateTicks, false);
    }
}
