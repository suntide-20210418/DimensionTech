package com.suntide_20210418.dimensiontech.structurereactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Cycle-level coverage for the V1 plan: the fixed reward window, the one-hundred-tick wait rule,
 * all five penalties, reward doubling and the separation between this cycle's recursion and the
 * recursion carried over from an already settled cycle.
 *
 * <p>The machine is generic over the supplied stack, so these tests drive it with plain named
 * tokens; no game registries, levels or block entities are involved.
 */
class StructureReactorCycleTest {
    private static final String BRANCH = "branch";
    private static final String RECURSE = "recurse";
    private static final String CONVERGE = "converge";
    private static final String STABILIZE = "stabilize";
    private static final String WRONG = "wrong";

    /** branch -> converge -> stabilize. */
    private static final List<StateStep<String>> SHALLOW = sequence(BRANCH, CONVERGE, STABILIZE);

    /** branch -> recurse -> recurse -> converge -> stabilize. */
    private static final List<StateStep<String>> DEPTH_TWO =
            sequence(BRANCH, RECURSE, RECURSE, CONVERGE, STABILIZE);

    /** branch -> recurse -> recurse -> recurse -> converge -> stabilize. */
    private static final List<StateStep<String>> DEPTH_THREE =
            sequence(BRANCH, RECURSE, RECURSE, RECURSE, CONVERGE, STABILIZE);

    /** A DSL whose converge slot sits after a recurse slot, so stabilize can be reached early. */
    private static final List<StateStep<String>> LATE_CONVERGE =
            sequence(BRANCH, RECURSE, RECURSE, RECURSE, CONVERGE, STABILIZE);

    private static final List<StateStep<String>> TIER_FIVE_A =
            sequence(BRANCH, RECURSE, RECURSE, CONVERGE, STABILIZE);

    private static final List<StateStep<String>> TIER_FIVE_B =
            sequence(BRANCH, RECURSE, BRANCH, RECURSE, CONVERGE, STABILIZE);

    @Test
    void rewardWindowIsInclusiveAtTwentyAndSixtyTicks() {
        assertEquals(
                StructureReactorCycle.Resolution.CORRECT_REWARDED,
                resolveAfterTicks(20, SHALLOW, BRANCH));
        assertEquals(
                StructureReactorCycle.Resolution.CORRECT_REWARDED,
                resolveAfterTicks(60, SHALLOW, BRANCH));
    }

    @Test
    void correctInputsOutsideTheWindowResolveWithoutReward() {
        for (int ticks : new int[] {0, 19, 61}) {
            assertEquals(
                    StructureReactorCycle.Resolution.CORRECT,
                    resolveAfterTicks(ticks, SHALLOW, BRANCH),
                    "tick " + ticks + " must settle without a reward");
        }
    }

    @Test
    void correctInputAtOneHundredOneTicksTriggersPhaseIdle() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 101);

        assertEquals(StructureReactorCycle.Resolution.PHASE_IDLE, cycle.resolve(BRANCH));
        assertEquals(0, cycle.stateIndex(), "phase idle must not advance the state");
        assertEquals(460, cycle.finalResult().timeTicks(), "phase idle must add sixty ticks");
    }

    @Test
    void stateTimerStopsAtTheTimeoutBoundary() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, StructureReactorCycle.STATE_TIMEOUT_TICKS + 20);

        assertEquals(StructureReactorCycle.STATE_TIMEOUT_TICKS, cycle.stateTicks());
        assertEquals(
                StructureReactorCycle.Resolution.PHASE_IDLE,
                cycle.resolve(BRANCH),
                "the timeout is settled exactly at the boundary");
        assertEquals(0, cycle.stateTicks());
    }

    @Test
    void everyResolvedInputIsConsumed() {
        assertTrue(resolveAfterTicks(20, SHALLOW, BRANCH).consumesInput());

        assertTrue(started(SHALLOW, null).resolve(BRANCH).consumesInput());
        assertTrue(started(SHALLOW, null).resolve(WRONG).consumesInput());

        StructureReactorCycle<String> late = started(SHALLOW, null);
        advance(late, 101);
        assertTrue(late.resolve(BRANCH).consumesInput());

        for (StructureReactorCycle.Resolution resolution :
                StructureReactorCycle.Resolution.values()) {
            if (resolution == StructureReactorCycle.Resolution.NONE) {
                assertFalse(resolution.consumesInput(), "an empty slot must never be consumed");
            } else {
                assertTrue(resolution.consumesInput(), resolution + " must consume its input");
            }
        }
    }

    @Test
    void emptyInputNeitherSettlesNorConsumes() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 20);

        assertEquals(StructureReactorCycle.Resolution.NONE, cycle.resolve((String) null));
        assertEquals(StructureReactorCycle.Resolution.NONE, cycle.resolve(null, true));
        assertEquals(0, cycle.stateIndex(), "an empty slot must keep the current state");
        assertEquals(20, cycle.stateTicks(), "an empty slot must not reset the wait timer");
        assertEquals(400, cycle.finalResult().timeTicks(), "an empty slot must not penalize");
    }

    /**
     * The tick loop asks once per tick, so an idle reactor submits an empty slot sixty times in a
     * row. The wait timer has to survive that and reach the reward window.
     */
    @Test
    void anIdleReactorStillReachesItsRewardWindow() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        for (int tick = 0; tick < StructureReactorCycle.REWARD_END_TICK; tick++) {
            cycle.tick();
            assertEquals(StructureReactorCycle.Resolution.NONE, cycle.resolve(null, true));
        }

        assertEquals(StructureReactorCycle.REWARD_END_TICK, cycle.stateTicks());
        assertEquals(
                StructureReactorCycle.Resolution.CORRECT_REWARDED,
                cycle.resolve(BRANCH, false),
                "waiting without inserting anything must still earn the reward");
    }

    /**
     * The reward window only measures the current state, so a display layer needs a separate
     * whole-cycle counter for the ritual's elapsed time.
     */
    @Test
    void elapsedTicksSurviveStateChangesAndRestartWithTheNextCycle() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 30);
        cycle.resolve(BRANCH);

        assertEquals(0, cycle.stateTicks(), "settling a state restarts its own window");
        assertEquals(0, cycle.elapsedTicks(), "refining must not start during item input states");
        advance(cycle, 10);
        assertEquals(10, cycle.stateTicks());
        assertEquals(0, cycle.elapsedTicks());

        cycle.abort(false);
        assertEquals(0, cycle.elapsedTicks(), "a rolled back cycle has no elapsed time");
    }

    @Test
    void refiningStartsOnlyAfterEveryOperationAndCompletesAtTheFinalTime() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        resolve(cycle, BRANCH);
        resolve(cycle, CONVERGE);
        resolve(cycle, STABILIZE);

        assertEquals(StructureReactorCycle.Status.REFINING, cycle.status());
        assertEquals(0, cycle.elapsedTicks());
        advance(cycle, cycle.finalResult().timeTicks() - 1);
        assertEquals(StructureReactorCycle.Status.REFINING, cycle.status());
        assertEquals(cycle.finalResult().timeTicks() - 1, cycle.elapsedTicks());

        cycle.tick();
        assertEquals(StructureReactorCycle.Status.READY_TO_COMMIT, cycle.status());
        assertEquals(cycle.finalResult().timeTicks(), cycle.elapsedTicks());
    }

    @Test
    void wrongInputAtOneHundredOneTicksTriggersPhaseIdle() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 101);

        assertEquals(StructureReactorCycle.Resolution.PHASE_IDLE, cycle.resolve(WRONG));
        assertEquals(0, cycle.stateIndex());
        assertEquals(0, cycle.extraFragmentCost(), "phase idle must not charge extra fragments");
        assertEquals(460, cycle.finalResult().timeTicks());
    }

    @Test
    void earlyConvergeChargesTenPercentPerMissingDepth() {
        // Depth three with only one settled recurse leaves two missing depth levels at converge.
        StructureReactorCycle<String> cycle = started(DEPTH_THREE, null);
        resolve(cycle, BRANCH);
        resolve(cycle, RECURSE);

        assertEquals(StructureReactorCycle.Resolution.EARLY_CONVERGE, resolve(cycle, CONVERGE));
        assertEquals(
                StateId.STABILIZE,
                cycle.currentState(),
                "the submitted converge settles the convergence point");
        assertEquals(
                800,
                cycle.finalResult().outputAmountMb(),
                "two missing depth levels must remove twenty percent");
    }

    @Test
    void wrongRecurseNeitherOverflowsNorChargesAFluidPenalty() {
        StructureReactorCycle<String> cycle = started(DEPTH_TWO, null);
        resolve(cycle, BRANCH);
        resolve(cycle, RECURSE);
        resolve(cycle, RECURSE);
        assertEquals(StateId.CONVERGE, cycle.currentState());

        assertEquals(StructureReactorCycle.Resolution.PHASE_IDLE, resolve(cycle, RECURSE));
        assertEquals(
                1_000,
                cycle.finalResult().fluidCostMb(),
                "an unreachable overflow must not charge a fluid penalty");
        assertTrue(resolve(cycle, CONVERGE) == StructureReactorCycle.Resolution.CORRECT
                || resolve(cycle, CONVERGE) == StructureReactorCycle.Resolution.CORRECT_REWARDED);
    }

    @Test
    void stabilizePenaltyOnlyAppliesBeforeConverge() {
        StructureReactorCycle<String> cycle = started(LATE_CONVERGE, null);
        resolve(cycle, BRANCH);
        resolve(cycle, RECURSE);
        assertEquals(StateId.RECURSE, cycle.currentState());

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertEquals(
                    StructureReactorCycle.Resolution.STABILIZE_FAILURE,
                    resolve(cycle, STABILIZE),
                    "stabilize before converge must use its own penalty");
            assertEquals(
                    Math.min(2, attempt),
                    cycle.extraFragmentCost(),
                    "the extra fragment cost is capped at two hundred percent");
        }
        assertEquals(StateId.RECURSE, cycle.currentState(), "a failed stabilize keeps the state");
    }

    @Test
    void stabilizeAtConvergeIsCorrectNotAFailure() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        resolve(cycle, BRANCH);
        advance(cycle, 20);
        assertEquals(StructureReactorCycle.Resolution.CORRECT_REWARDED, resolve(cycle, CONVERGE));
        advance(cycle, 5);
        assertEquals(StructureReactorCycle.Resolution.CORRECT, resolve(cycle, STABILIZE));
        assertEquals(0, cycle.extraFragmentCost(), "a correct stabilize costs no fragments");
        assertEquals(StructureReactorCycle.Status.REFINING, cycle.status());
    }

    @Test
    void branchConflictPenalizesEightyTicksAndRewindsToBranch() {
        StructureReactorCycle<String> cycle = started(TIER_FIVE_A, TIER_FIVE_B);
        resolve(cycle, BRANCH);
        resolve(cycle, RECURSE);

        assertEquals(StructureReactorCycle.Resolution.BRANCH_CONFLICT, resolve(cycle, BRANCH));
        assertEquals(0, cycle.stateIndex(), "a conflict rewinds to the branch state");
        assertEquals(StructureReactorRecipe.Branch.A, cycle.branch(), "a conflict must not switch");
        assertEquals(480, cycle.finalResult().timeTicks(), "a conflict adds eighty ticks");
    }

    @Test
    void wrongOperationAtTheSharedHeadIsAnOrdinaryWrongInput() {
        StructureReactorCycle<String> cycle = started(TIER_FIVE_A, TIER_FIVE_B);
        assertEquals(StructureReactorCycle.Resolution.PHASE_IDLE, resolve(cycle, RECURSE));
        assertEquals(460, cycle.finalResult().timeTicks());
    }

    @Test
    void stabilizeDoublesThisCyclesBankedRewards() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 20);
        resolve(cycle, BRANCH);
        advance(cycle, 20);
        resolve(cycle, CONVERGE);
        advance(cycle, 60);
        assertEquals(StructureReactorCycle.Resolution.CORRECT_REWARDED, resolve(cycle, STABILIZE));
        assertEquals(StructureReactorCycle.Status.REFINING, cycle.status());

        ReactorFormula.Result result = cycle.finalResult();
        assertEquals(320, result.timeTicks(), "the forty reduction ticks doubled");
        assertEquals(1_000, result.fluidCostMb(), "the branch reward does not touch fluid");
        assertEquals(1_500, result.outputAmountMb(), "the twenty-five percent bonus doubled");
    }

    @Test
    void stabilizeDoesNotDoublePenalties() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 101);
        assertEquals(StructureReactorCycle.Resolution.PHASE_IDLE, resolve(cycle, WRONG));
        assertEquals(460, cycle.finalResult().timeTicks(), "the penalty stays a single sixty ticks");
    }

    @Test
    void carriedRecursionSurvivesACycleWithoutBeingDoubledAgain() {
        StructureReactorRecipe<String> recipe = recipe(DEPTH_THREE, null);
        StructureReactorCycle<String> cycle = new StructureReactorCycle<>();
        cycle.start(recipe);
        runRewardedRecursionCycle(cycle, recipe);
        assertEquals(1, cycle.carriedExtraRecursion(), "the first cycle banks one recursion");
        runRewardedRecursionCycle(cycle, recipe);
        assertEquals(
                1,
                cycle.carriedExtraRecursion(),
                "an unconsumed carried recursion must survive the next cycle");
    }

    @Test
    void carriedRecursionIsCappedAtFour() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        CompoundTag tag = new CompoundTag();
        cycle.save(tag);
        tag.putInt("ReactorCarriedRecursion", 9);
        cycle.loadPersistent(tag, id -> null);
        assertEquals(4, cycle.carriedExtraRecursion(), "the carried recursion is capped at four");
    }

    @Test
    void legacyUnsettledCarryIsSettledOnceAndThenFrozen() {
        StructureReactorRecipe<String> recipe = recipe(DEPTH_THREE, null);
        StructureReactorCycle<String> cycle = new StructureReactorCycle<>();
        cycle.start(recipe);
        runRewardedRecursionCycle(cycle, recipe);
        assertEquals(1, cycle.carriedExtraRecursion());

        CompoundTag tag = new CompoundTag();
        cycle.save(tag);
        tag.putBoolean("ReactorCarriedSettled", false);
        cycle.loadPersistent(tag, id -> recipe);
        assertEquals(1, cycle.carriedExtraRecursion(), "an unsettled carry stays unsettled");

        cycle.start(recipe);
        runRewardedRecursionCycle(cycle, recipe);
        assertEquals(
                1,
                cycle.carriedExtraRecursion(),
                "the legacy carry is spent by the next cycle's recurse slot and never doubled again");
    }

    @Test
    void changingRecipeClearsRecursionAndResetsBranch() {
        StructureReactorRecipe<String> tierFive = recipe(TIER_FIVE_A, TIER_FIVE_B);
        StructureReactorCycle<String> cycle = new StructureReactorCycle<>();
        cycle.start(tierFive);
        runFullCycle(cycle);
        assertEquals(StructureReactorRecipe.Branch.B, cycle.branch(), "commit must alternate");

        cycle.abort(true);
        assertEquals(StructureReactorRecipe.Branch.A, cycle.branch(), "a recipe change resets A");
        assertEquals(0, cycle.carriedExtraRecursion(), "a recipe change clears recursion");
        assertEquals(StructureReactorCycle.Status.IDLE, cycle.status());

        cycle.start(tierFive);
        assertEquals(0, cycle.carriedExtraRecursion(), "the new cycle starts without recursion");
    }

    @Test
    void penaltiesReachTheFinalFormulaAndStayInsideTheV1Bounds() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 101);
        resolve(cycle, BRANCH);
        resolve(cycle, WRONG);
        resolve(cycle, STABILIZE);

        ReactorFormula.Result result = cycle.finalResult();
        assertTrue(result.timeTicks() > 400, "collected penalties must reach the final time");
        assertTrue(result.timeTicks() >= 100 && result.timeTicks() <= 800);
        assertTrue(result.fluidCostMb() >= 250 && result.fluidCostMb() <= 2_000);
        assertTrue(result.outputAmountMb() >= 500 && result.outputAmountMb() <= 4_000);
    }

    @Test
    void reloadKeepsBranchAndSettledRecursion() {
        StructureReactorRecipe<String> tierFive = recipe(TIER_FIVE_A, TIER_FIVE_B);
        StructureReactorCycle<String> cycle = new StructureReactorCycle<>();
        cycle.start(tierFive);
        runFullCycle(cycle);
        assertEquals(StructureReactorRecipe.Branch.B, cycle.branch());

        CompoundTag tag = new CompoundTag();
        cycle.save(tag);
        StructureReactorCycle<String> reloaded = new StructureReactorCycle<>();
        reloaded.loadPersistent(tag, id -> tierFive);
        assertEquals(StructureReactorRecipe.Branch.B, reloaded.branch(), "a reload keeps the branch");
        assertEquals(StructureReactorCycle.Status.IDLE, reloaded.status());
    }

    @Test
    void startingTwiceIsRejected() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        assertThrows(IllegalStateException.class, () -> cycle.start(recipe(SHALLOW, null)));
    }

    @Test
    void committingAnUnfinishedCycleIsRejected() {
        StructureReactorCycle<String> cycle = started(SHALLOW, null);
        assertThrows(IllegalStateException.class, cycle::commit);
    }

    @Test
    void sequenceValidationRejectsMalformedDsl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> recipe(sequence(CONVERGE, STABILIZE), null),
                "the first state must be branch");
        assertThrows(
                IllegalArgumentException.class,
                () -> recipe(sequence(BRANCH, CONVERGE), null),
                "the last state must be stabilize");
        assertThrows(
                IllegalArgumentException.class,
                () -> recipe(sequence(BRANCH, STABILIZE), null),
                "converge must precede stabilize");
        assertThrows(
                IllegalArgumentException.class,
                () -> recipe(List.of(), null),
                "the sequence must not be empty");
    }

    /** Runs one cycle in which every state is hit inside the reward window and commits it. */
    private static void runRewardedRecursionCycle(
            StructureReactorCycle<String> cycle, StructureReactorRecipe<String> recipe) {
        runFullCycle(cycle, recipe, true);
        cycle.commit();
    }

    /** Runs one cycle through every state and commits it. */
    private static void runFullCycle(StructureReactorCycle<String> cycle) {
        runFullCycle(cycle, cycle.recipe(), false);
        cycle.commit();
    }

    private static void runFullCycle(
            StructureReactorCycle<String> cycle,
            StructureReactorRecipe<String> recipe,
            boolean expectRewarded) {
        if (cycle.status() != StructureReactorCycle.Status.RUNNING)
            cycle.start(recipe, cycle.branch());
        int guard = 0;
        while (cycle.status() == StructureReactorCycle.Status.RUNNING && guard++ < 16) {
            if (expectRewarded) advance(cycle, 20);
            String operation = nameOf(cycle.currentState());
            StructureReactorCycle.Resolution result = cycle.resolve(operation);
            if (expectRewarded) {
                assertEquals(
                        StructureReactorCycle.Resolution.CORRECT_REWARDED,
                        result,
                        "a rewarded input must land inside the window");
            } else {
                assertTrue(result.consumesInput(), "a supplied input must settle the state");
            }
        }
        assertEquals(StructureReactorCycle.Status.REFINING, cycle.status());
        advance(cycle, cycle.finalResult().timeTicks());
        assertEquals(
                StructureReactorCycle.Status.READY_TO_COMMIT,
                cycle.status(),
                "the cycle must finish");
    }

    private static StructureReactorCycle.Resolution resolveAfterTicks(
            int ticks, List<StateStep<String>> sequence, String operation) {
        StructureReactorCycle<String> cycle = started(sequence, null);
        advance(cycle, ticks);
        return cycle.resolve(operation);
    }

    private static void advance(StructureReactorCycle<String> cycle, int ticks) {
        for (int i = 0; i < ticks; i++) cycle.tick();
    }

    private static StructureReactorCycle.Resolution resolve(
            StructureReactorCycle<String> cycle, String operation) {
        return cycle.resolve(operation);
    }

    private static StructureReactorCycle<String> started(
            List<StateStep<String>> sequence, List<StateStep<String>> alternate) {
        StructureReactorCycle<String> cycle = new StructureReactorCycle<>();
        cycle.start(recipe(sequence, alternate));
        return cycle;
    }

    private static StructureReactorRecipe<String> recipe(
            List<StateStep<String>> sequence, List<StateStep<String>> alternate) {
        return new StructureReactorRecipe<>(
                ResourceLocation.fromNamespaceAndPath("dimension_tech", "unit_test"),
                null,
                null,
                null,
                1,
                1_000,
                1_000,
                sequence,
                alternate);
    }

    private static String nameOf(StateId state) {
        return switch (state) {
            case BRANCH -> BRANCH;
            case RECURSE -> RECURSE;
            case CONVERGE -> CONVERGE;
            case STABILIZE -> STABILIZE;
        };
    }

    private static List<StateStep<String>> sequence(String... names) {
        StateStep<String>[] steps = new StateStep[names.length];
        for (int i = 0; i < names.length; i++) {
            StateId state = switch (names[i]) {
                case BRANCH -> StateId.BRANCH;
                case RECURSE -> StateId.RECURSE;
                case CONVERGE -> StateId.CONVERGE;
                case STABILIZE -> StateId.STABILIZE;
                default -> throw new IllegalArgumentException("unknown state " + names[i]);
            };
            String expected = names[i];
            steps[i] =
                    new StateStep<>(
                            state,
                            OperationMatcher.of(candidate -> expected.equals(candidate)));
        }
        return List.of(steps);
    }
}
