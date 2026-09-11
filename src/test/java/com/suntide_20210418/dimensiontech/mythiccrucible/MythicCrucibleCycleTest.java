package com.suntide_20210418.dimensiontech.mythiccrucible;

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
class MythicCrucibleCycleTest {
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
                MythicCrucibleCycle.Resolution.CORRECT_REWARDED,
                resolveAfterTicks(20, SHALLOW, BRANCH));
        assertEquals(
                MythicCrucibleCycle.Resolution.CORRECT_REWARDED,
                resolveAfterTicks(60, SHALLOW, BRANCH));
    }

    @Test
    void correctInputsOutsideTheWindowResolveWithoutReward() {
        for (int ticks : new int[] {0, 19, 61, 100}) {
            assertEquals(
                    MythicCrucibleCycle.Resolution.CORRECT,
                    resolveAfterTicks(ticks, SHALLOW, BRANCH),
                    "tick " + ticks + " must settle without a reward");
        }
    }

    @Test
    void correctInputAtOneHundredOneTicksTriggersPhaseIdle() {
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 101);

        assertEquals(MythicCrucibleCycle.Resolution.PHASE_IDLE, cycle.resolve(BRANCH));
        assertEquals(0, cycle.stateIndex(), "phase idle must not advance the state");
        assertEquals(460, cycle.finalResult().timeTicks(), "phase idle must add sixty ticks");
    }

    @Test
    void everyResolvedInputIsConsumed() {
        assertTrue(resolveAfterTicks(20, SHALLOW, BRANCH).consumesInput());

        assertTrue(started(SHALLOW, null).resolve(BRANCH).consumesInput());
        assertTrue(started(SHALLOW, null).resolve(WRONG).consumesInput());

        MythicCrucibleCycle<String> late = started(SHALLOW, null);
        advance(late, 101);
        assertTrue(late.resolve(BRANCH).consumesInput());

        for (MythicCrucibleCycle.Resolution resolution :
                MythicCrucibleCycle.Resolution.values()) {
            if (resolution == MythicCrucibleCycle.Resolution.NONE) {
                assertFalse(resolution.consumesInput(), "an empty slot must never be consumed");
            } else {
                assertTrue(resolution.consumesInput(), resolution + " must consume its input");
            }
        }
    }

    @Test
    void emptyInputNeitherSettlesNorConsumes() {
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 20);

        assertEquals(MythicCrucibleCycle.Resolution.NONE, cycle.resolve((String) null));
        assertEquals(MythicCrucibleCycle.Resolution.NONE, cycle.resolve(null, true));
        assertEquals(0, cycle.stateIndex(), "an empty slot must keep the current state");
        assertEquals(20, cycle.stateTicks(), "an empty slot must not reset the wait timer");
        assertEquals(400, cycle.finalResult().timeTicks(), "an empty slot must not penalize");
    }

    @Test
    void wrongInputAtOneHundredOneTicksTriggersPhaseIdle() {
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 101);

        assertEquals(MythicCrucibleCycle.Resolution.PHASE_IDLE, cycle.resolve(WRONG));
        assertEquals(0, cycle.stateIndex());
        assertEquals(0, cycle.extraFragmentCost(), "phase idle must not charge extra fragments");
        assertEquals(460, cycle.finalResult().timeTicks());
    }

    @Test
    void earlyConvergeChargesTenPercentPerMissingDepth() {
        // Depth three with only one settled recurse leaves two missing depth levels at converge.
        MythicCrucibleCycle<String> cycle = started(DEPTH_THREE, null);
        resolve(cycle, BRANCH);
        resolve(cycle, RECURSE);

        assertEquals(MythicCrucibleCycle.Resolution.EARLY_CONVERGE, resolve(cycle, CONVERGE));
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
        MythicCrucibleCycle<String> cycle = started(DEPTH_TWO, null);
        resolve(cycle, BRANCH);
        resolve(cycle, RECURSE);
        resolve(cycle, RECURSE);
        assertEquals(StateId.CONVERGE, cycle.currentState());

        assertEquals(MythicCrucibleCycle.Resolution.PHASE_IDLE, resolve(cycle, RECURSE));
        assertEquals(
                1_000,
                cycle.finalResult().fluidCostMb(),
                "an unreachable overflow must not charge a fluid penalty");
        assertTrue(resolve(cycle, CONVERGE) == MythicCrucibleCycle.Resolution.CORRECT
                || resolve(cycle, CONVERGE) == MythicCrucibleCycle.Resolution.CORRECT_REWARDED);
    }

    @Test
    void stabilizePenaltyOnlyAppliesBeforeConverge() {
        MythicCrucibleCycle<String> cycle = started(LATE_CONVERGE, null);
        resolve(cycle, BRANCH);
        resolve(cycle, RECURSE);
        assertEquals(StateId.RECURSE, cycle.currentState());

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertEquals(
                    MythicCrucibleCycle.Resolution.STABILIZE_FAILURE,
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
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        resolve(cycle, BRANCH);
        advance(cycle, 20);
        assertEquals(MythicCrucibleCycle.Resolution.CORRECT_REWARDED, resolve(cycle, CONVERGE));
        advance(cycle, 5);
        assertEquals(MythicCrucibleCycle.Resolution.CORRECT, resolve(cycle, STABILIZE));
        assertEquals(0, cycle.extraFragmentCost(), "a correct stabilize costs no fragments");
        assertEquals(MythicCrucibleCycle.Status.READY_TO_COMMIT, cycle.status());
    }

    @Test
    void branchConflictPenalizesEightyTicksAndRewindsToBranch() {
        MythicCrucibleCycle<String> cycle = started(TIER_FIVE_A, TIER_FIVE_B);
        resolve(cycle, BRANCH);
        resolve(cycle, RECURSE);

        assertEquals(MythicCrucibleCycle.Resolution.BRANCH_CONFLICT, resolve(cycle, BRANCH));
        assertEquals(0, cycle.stateIndex(), "a conflict rewinds to the branch state");
        assertEquals(MythicCrucibleRecipe.Branch.A, cycle.branch(), "a conflict must not switch");
        assertEquals(480, cycle.finalResult().timeTicks(), "a conflict adds eighty ticks");
    }

    @Test
    void wrongOperationAtTheSharedHeadIsAnOrdinaryWrongInput() {
        MythicCrucibleCycle<String> cycle = started(TIER_FIVE_A, TIER_FIVE_B);
        assertEquals(MythicCrucibleCycle.Resolution.PHASE_IDLE, resolve(cycle, RECURSE));
        assertEquals(460, cycle.finalResult().timeTicks());
    }

    @Test
    void stabilizeDoublesThisCyclesBankedRewards() {
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 20);
        resolve(cycle, BRANCH);
        advance(cycle, 20);
        resolve(cycle, CONVERGE);
        advance(cycle, 60);
        assertEquals(MythicCrucibleCycle.Resolution.CORRECT_REWARDED, resolve(cycle, STABILIZE));
        assertEquals(MythicCrucibleCycle.Status.READY_TO_COMMIT, cycle.status());

        CrucibleFormula.Result result = cycle.finalResult();
        assertEquals(320, result.timeTicks(), "the forty reduction ticks doubled");
        assertEquals(1_000, result.fluidCostMb(), "the branch reward does not touch fluid");
        assertEquals(1_500, result.outputAmountMb(), "the twenty-five percent bonus doubled");
    }

    @Test
    void stabilizeDoesNotDoublePenalties() {
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 101);
        assertEquals(MythicCrucibleCycle.Resolution.PHASE_IDLE, resolve(cycle, WRONG));
        assertEquals(460, cycle.finalResult().timeTicks(), "the penalty stays a single sixty ticks");
    }

    @Test
    void carriedRecursionSurvivesACycleWithoutBeingDoubledAgain() {
        MythicCrucibleRecipe<String> recipe = recipe(DEPTH_THREE, null);
        MythicCrucibleCycle<String> cycle = new MythicCrucibleCycle<>();
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
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        CompoundTag tag = new CompoundTag();
        cycle.save(tag);
        tag.putInt("CrucibleCarriedRecursion", 9);
        cycle.loadPersistent(tag, id -> null);
        assertEquals(4, cycle.carriedExtraRecursion(), "the carried recursion is capped at four");
    }

    @Test
    void legacyUnsettledCarryIsSettledOnceAndThenFrozen() {
        MythicCrucibleRecipe<String> recipe = recipe(DEPTH_THREE, null);
        MythicCrucibleCycle<String> cycle = new MythicCrucibleCycle<>();
        cycle.start(recipe);
        runRewardedRecursionCycle(cycle, recipe);
        assertEquals(1, cycle.carriedExtraRecursion());

        CompoundTag tag = new CompoundTag();
        cycle.save(tag);
        tag.putBoolean("CrucibleCarriedSettled", false);
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
        MythicCrucibleRecipe<String> tierFive = recipe(TIER_FIVE_A, TIER_FIVE_B);
        MythicCrucibleCycle<String> cycle = new MythicCrucibleCycle<>();
        cycle.start(tierFive);
        runFullCycle(cycle);
        assertEquals(MythicCrucibleRecipe.Branch.B, cycle.branch(), "commit must alternate");

        cycle.abort(true);
        assertEquals(MythicCrucibleRecipe.Branch.A, cycle.branch(), "a recipe change resets A");
        assertEquals(0, cycle.carriedExtraRecursion(), "a recipe change clears recursion");
        assertEquals(MythicCrucibleCycle.Status.IDLE, cycle.status());

        cycle.start(tierFive);
        assertEquals(0, cycle.carriedExtraRecursion(), "the new cycle starts without recursion");
    }

    @Test
    void penaltiesReachTheFinalFormulaAndStayInsideTheV1Bounds() {
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        advance(cycle, 101);
        resolve(cycle, BRANCH);
        resolve(cycle, WRONG);
        resolve(cycle, STABILIZE);

        CrucibleFormula.Result result = cycle.finalResult();
        assertTrue(result.timeTicks() > 400, "collected penalties must reach the final time");
        assertTrue(result.timeTicks() >= 100 && result.timeTicks() <= 800);
        assertTrue(result.fluidCostMb() >= 250 && result.fluidCostMb() <= 2_000);
        assertTrue(result.outputAmountMb() >= 500 && result.outputAmountMb() <= 4_000);
    }

    @Test
    void reloadKeepsBranchAndSettledRecursion() {
        MythicCrucibleRecipe<String> tierFive = recipe(TIER_FIVE_A, TIER_FIVE_B);
        MythicCrucibleCycle<String> cycle = new MythicCrucibleCycle<>();
        cycle.start(tierFive);
        runFullCycle(cycle);
        assertEquals(MythicCrucibleRecipe.Branch.B, cycle.branch());

        CompoundTag tag = new CompoundTag();
        cycle.save(tag);
        MythicCrucibleCycle<String> reloaded = new MythicCrucibleCycle<>();
        reloaded.loadPersistent(tag, id -> tierFive);
        assertEquals(MythicCrucibleRecipe.Branch.B, reloaded.branch(), "a reload keeps the branch");
        assertEquals(MythicCrucibleCycle.Status.IDLE, reloaded.status());
    }

    @Test
    void startingTwiceIsRejected() {
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
        assertThrows(IllegalStateException.class, () -> cycle.start(recipe(SHALLOW, null)));
    }

    @Test
    void committingAnUnfinishedCycleIsRejected() {
        MythicCrucibleCycle<String> cycle = started(SHALLOW, null);
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
            MythicCrucibleCycle<String> cycle, MythicCrucibleRecipe<String> recipe) {
        runFullCycle(cycle, recipe, true);
        cycle.commit();
    }

    /** Runs one cycle through every state and commits it. */
    private static void runFullCycle(MythicCrucibleCycle<String> cycle) {
        runFullCycle(cycle, cycle.recipe(), false);
        cycle.commit();
    }

    private static void runFullCycle(
            MythicCrucibleCycle<String> cycle,
            MythicCrucibleRecipe<String> recipe,
            boolean expectRewarded) {
        if (cycle.status() != MythicCrucibleCycle.Status.RUNNING)
            cycle.start(recipe, cycle.branch());
        int guard = 0;
        while (cycle.status() == MythicCrucibleCycle.Status.RUNNING && guard++ < 16) {
            if (expectRewarded) advance(cycle, 20);
            String operation = nameOf(cycle.currentState());
            MythicCrucibleCycle.Resolution result = cycle.resolve(operation);
            if (expectRewarded) {
                assertEquals(
                        MythicCrucibleCycle.Resolution.CORRECT_REWARDED,
                        result,
                        "a rewarded input must land inside the window");
            } else {
                assertTrue(result.consumesInput(), "a supplied input must settle the state");
            }
        }
        assertEquals(
                MythicCrucibleCycle.Status.READY_TO_COMMIT,
                cycle.status(),
                "the cycle must finish");
    }

    private static MythicCrucibleCycle.Resolution resolveAfterTicks(
            int ticks, List<StateStep<String>> sequence, String operation) {
        MythicCrucibleCycle<String> cycle = started(sequence, null);
        advance(cycle, ticks);
        return cycle.resolve(operation);
    }

    private static void advance(MythicCrucibleCycle<String> cycle, int ticks) {
        for (int i = 0; i < ticks; i++) cycle.tick();
    }

    private static MythicCrucibleCycle.Resolution resolve(
            MythicCrucibleCycle<String> cycle, String operation) {
        return cycle.resolve(operation);
    }

    private static MythicCrucibleCycle<String> started(
            List<StateStep<String>> sequence, List<StateStep<String>> alternate) {
        MythicCrucibleCycle<String> cycle = new MythicCrucibleCycle<>();
        cycle.start(recipe(sequence, alternate));
        return cycle;
    }

    private static MythicCrucibleRecipe<String> recipe(
            List<StateStep<String>> sequence, List<StateStep<String>> alternate) {
        return new MythicCrucibleRecipe<>(
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
