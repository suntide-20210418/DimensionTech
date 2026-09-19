package com.suntide_20210418.dimensiontech.structurereactor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-side finite-state machine for one reactor. It deliberately owns no inventory or fluid
 * containers: callers reserve/commit resources around this durable state.
 */
public final class StructureReactorCycle<S> {
    public static final int STATE_TIMEOUT_TICKS = 100;
    public static final int REWARD_START_TICK = 20;
    public static final int REWARD_END_TICK = 60;
    public static final int MAX_CARRIED_EXTRA_RECURSION = 4;

    /** Waiting at or past {@link #STATE_TIMEOUT_TICKS}, or submitting an unknown operation. */
    public static final int WRONG_STATE_TIME_PENALTY_TICKS = 60;

    /** Submitting the other branch's operation at the same index. */
    public static final int BRANCH_CONFLICT_TIME_PENALTY_TICKS = 80;

    /** Submitting recurse below the target depth. */
    public static final int RECURSION_OVERFLOW_FLUID_PENALTY_BP = 2_500;

    /** Submitting converge before the target depth, charged per missing depth level. */
    public static final int EARLY_CONVERGE_OUTPUT_PENALTY_BP_PER_DEPTH = 1_000;

    /** Rewarded branch. */
    public static final int REWARD_BRANCH_TIME_REDUCTION_TICKS = 40;

    /** Rewarded recurse: cheaper fluid plus one extra recursion advance. */
    public static final int REWARD_RECURSE_FLUID_REDUCTION_BP = 1_000;

    /** Rewarded converge. */
    public static final int REWARD_CONVERGE_OUTPUT_BONUS_BP = 2_500;

    /** Submitting stabilize while the cycle is not ready to stabilize. */
    public static final int STABILIZE_FAILURE_EXTRA_FRAGMENTS = 1;

    /** A failed stabilize may at most double the recipe's fragment cost. */
    public static final int MAX_EXTRA_FRAGMENT_MULTIPLIER = 2;

    private StructureReactorRecipe<S> recipe;
    private StructureReactorRecipe.Branch branch = StructureReactorRecipe.Branch.A;
    private int stateIndex;
    private int stateTicks;

    /** Ticks spent in the dedicated refining phase. */
    private int elapsedTicks;

    private int currentDepth;
    private int timeReduction;
    private int timePenalty;
    private int fluidReductionBp;
    private int fluidPenaltyBp;
    private int outputBonusBp;
    private int outputPenaltyBp;
    private int currentCycleExtraRecursion;
    private int pendingCarriedRecursion;
    private int carriedExtraRecursion;
    private boolean carriedRecursionAlreadySettled = true;
    private int extraFragmentCost;
    /** Outcome recorded for each settled sequence step; {@code NONE} until a step settles. */
    private final List<Resolution> stateOutcomes = new ArrayList<>();
    private Status status = Status.IDLE;

    /** Starts a cycle that keeps the branch persisted on this state machine. */
    public void start(StructureReactorRecipe<S> recipe) {
        start(recipe, branch);
    }

    /**
     * Starts a cycle on an explicit branch. Tier five alternates its DSL every completed cycle, so
     * callers may restore a persisted branch before starting.
     */
    public void start(StructureReactorRecipe<S> recipe, StructureReactorRecipe.Branch startBranch) {
        if (status != Status.IDLE) throw new IllegalStateException("a cycle is already active");
        if (recipe.hasAlternateBranch()) {
            if (startBranch == null) throw new IllegalArgumentException("branch must not be null");
            branch = startBranch;
        } else {
            branch = StructureReactorRecipe.Branch.A;
        }
        this.recipe = recipe;
        stateIndex = stateTicks = currentDepth = 0;
        stateOutcomes.clear();
        elapsedTicks = 0;
        timeReduction =
                timePenalty =
                        fluidReductionBp = fluidPenaltyBp = outputBonusBp = outputPenaltyBp = 0;
        currentCycleExtraRecursion = pendingCarriedRecursion = extraFragmentCost = 0;
        status = Status.RUNNING;
    }

    /** Advance the waiting timer; callers resolve an inserted item during the same server tick. */
    public void tick() {
        if (status == Status.RUNNING) {
            stateTicks = Math.min(STATE_TIMEOUT_TICKS, stateTicks + 1);
        } else if (status == Status.REFINING) {
            elapsedTicks++;
            if (elapsedTicks >= finalResult().timeTicks()) status = Status.READY_TO_COMMIT;
        }
    }

    /** Resolves a possibly empty supplied stack. */
    public Resolution resolve(S stack) {
        return resolve(stack, stack == null);
    }

    /** Resolves exactly one supplied operation stack. Empty stacks are intentionally ignored. */
    public Resolution resolve(S stack, boolean inputEmpty) {
        if (status != Status.RUNNING || inputEmpty) return Resolution.NONE;
        int actionIndex = stateIndex;
        if (stateTicks >= STATE_TIMEOUT_TICKS) {
            timePenalty += WRONG_STATE_TIME_PENALTY_TICKS;
            stateTicks = 0;
            recordOutcome(actionIndex, Resolution.PHASE_IDLE);
            return Resolution.PHASE_IDLE;
        }
        StateStep<S> expected = currentStep();
        StateId expectedState = expected.state();
        StateId submitted = stateFor(stack);
        if (submitted == expectedState && expected.operation().test(stack)) return correct(actionIndex);
        if (submitted == null) {
            timePenalty += WRONG_STATE_TIME_PENALTY_TICKS;
            stateTicks = 0;
            recordOutcome(actionIndex, Resolution.PHASE_IDLE);
            return Resolution.PHASE_IDLE;
        }
        int targetDepth = recipe.targetDepth(branch);
        // Each penalty is selected by a switch on the submitted operation and, where it matters,
        // on the current state, so the routing never depends on a shared comparison result.
        switch (submitted) {
            case STABILIZE -> {
                switch (expectedState) {
                    case CONVERGE -> {}
                    case BRANCH, RECURSE, STABILIZE -> {
                        extraFragmentCost =
                                Math.min(
                                        recipe.fragmentCount() * MAX_EXTRA_FRAGMENT_MULTIPLIER,
                                        extraFragmentCost + STABILIZE_FAILURE_EXTRA_FRAGMENTS);
                        stateTicks = 0;
                        recordOutcome(actionIndex, Resolution.STABILIZE_FAILURE);
                        return Resolution.STABILIZE_FAILURE;
                    }
                }
            }
            case CONVERGE -> {
                if (currentDepth < targetDepth) {
                    outputPenaltyBp +=
                            EARLY_CONVERGE_OUTPUT_PENALTY_BP_PER_DEPTH
                                    * (targetDepth - currentDepth);
                    switch (expectedState) {
                        case CONVERGE -> stateIndex++;
                        case BRANCH, RECURSE, STABILIZE -> stateIndex = convergeIndex() + 1;
                    }
                    stateTicks = 0;
                    settleStep();
                    recordOutcome(actionIndex, Resolution.EARLY_CONVERGE);
                    return Resolution.EARLY_CONVERGE;
                }
            }
            case RECURSE -> {
                if (currentDepth > targetDepth) {
                    fluidPenaltyBp += RECURSION_OVERFLOW_FLUID_PENALTY_BP;
                    currentDepth = Math.max(0, targetDepth - 1);
                    stateTicks = 0;
                    recordOutcome(actionIndex, Resolution.RECURSION_OVERFLOW);
                    return Resolution.RECURSION_OVERFLOW;
                }
            }
            case BRANCH -> {}
        }
        if (isBranchConflict(submitted)) {
            timePenalty += BRANCH_CONFLICT_TIME_PENALTY_TICKS;
            stateIndex = branchIndex();
            stateTicks = 0;
            recordOutcome(actionIndex, Resolution.BRANCH_CONFLICT);
            return Resolution.BRANCH_CONFLICT;
        }
        timePenalty += WRONG_STATE_TIME_PENALTY_TICKS;
        stateTicks = 0;
        recordOutcome(actionIndex, Resolution.PHASE_IDLE);
        return Resolution.PHASE_IDLE;
    }

    private Resolution correct(int actionIndex) {
        boolean rewarded = stateTicks >= REWARD_START_TICK && stateTicks <= REWARD_END_TICK;
        StateId id = currentStep().state();
        if (rewarded) {
            switch (id) {
                case BRANCH -> timeReduction += REWARD_BRANCH_TIME_REDUCTION_TICKS;
                case RECURSE -> {
                    fluidReductionBp += REWARD_RECURSE_FLUID_REDUCTION_BP;
                    currentCycleExtraRecursion++;
                }
                case CONVERGE -> outputBonusBp += REWARD_CONVERGE_OUTPUT_BONUS_BP;
                case STABILIZE -> settleStabilizeReward();
            }
        }
        recordOutcome(
                actionIndex, rewarded ? Resolution.CORRECT_REWARDED : Resolution.CORRECT);
        if (id == StateId.RECURSE) currentDepth++;
        stateIndex++;
        stateTicks = 0;
        settleStep();
        if (stateIndex >= sequence().size()) status = Status.REFINING;
        return rewarded ? Resolution.CORRECT_REWARDED : Resolution.CORRECT;
    }

    /**
     * A reward window that lands on stabilize doubles this cycle's unsettled rewards exactly once.
     * {@code carriedExtraRecursion} is deliberately absent: recursion inherited from an earlier,
     * already settled cycle must never be doubled a second time.
     */
    private void settleStabilizeReward() {
        timeReduction *= 2;
        fluidReductionBp *= 2;
        outputBonusBp *= 2;
        currentCycleExtraRecursion *= 2;
    }

    /**
     * Spends one extra recursion whenever the cycle advances onto a recurse state. Every unspent
     * recursion is banked into {@code pendingCarriedRecursion} at the end of the cycle, so both
     * this cycle's leftovers and an inherited recursion that was consumed here are settled exactly
     * once.
     */
    private void settleStep() {
        if (stateIndex >= sequence().size()) {
            status = Status.REFINING;
            flushExtraRecursion();
            return;
        }
        if (currentStep().state() != StateId.RECURSE) {
            flushExtraRecursion();
            return;
        }
        // This cycle's own recursion is spent first, so an inherited one survives longer.
        if (currentCycleExtraRecursion > 0) currentCycleExtraRecursion--;
        else if (pendingCarriedRecursion > 0) pendingCarriedRecursion--;
        else if (carriedExtraRecursion > 0) {
            // Spending an already settled recursion settles it again for the next cycle.
            carriedExtraRecursion--;
            pendingCarriedRecursion++;
        } else {
            flushExtraRecursion();
            return;
        }
        currentDepth++;
        stateIndex++;
        stateTicks = 0;
        if (stateIndex >= sequence().size()) {
            status = Status.REFINING;
            flushExtraRecursion();
            return;
        }
        settleStep();
    }

    private void flushExtraRecursion() {
        pendingCarriedRecursion += currentCycleExtraRecursion;
        currentCycleExtraRecursion = 0;
    }

    /**
     * Called exactly after output and resource settlement succeed. Any recursion that was not spent
     * during the cycle is settled here and marked as already settled for the next cycle.
     */
    public void commit() {
        if (status != Status.READY_TO_COMMIT)
            throw new IllegalStateException("cycle is not complete");
        flushExtraRecursion();
        carriedExtraRecursion =
                Math.min(
                        MAX_CARRIED_EXTRA_RECURSION,
                        carriedExtraRecursion + pendingCarriedRecursion);
        carriedRecursionAlreadySettled = true;
        if (recipe.hasAlternateBranch()) branch = branch.next();
        clearCycle();
    }

    /**
     * Roll back an uncommitted cycle. Changing recipes additionally clears persistent
     * rewards/branch.
     */
    public void abort(boolean changingRecipe) {
        clearCycle();
        if (changingRecipe) {
            carriedExtraRecursion = 0;
            carriedRecursionAlreadySettled = true;
            branch = StructureReactorRecipe.Branch.A;
        }
    }

    private void clearCycle() {
        recipe = null;
        status = Status.IDLE;
        stateIndex = stateTicks = currentDepth = 0;
        elapsedTicks = 0;
        timeReduction =
                timePenalty =
                        fluidReductionBp = fluidPenaltyBp = outputBonusBp = outputPenaltyBp = 0;
        currentCycleExtraRecursion = pendingCarriedRecursion = extraFragmentCost = 0;
    }

    private StateId stateFor(S stack) {
        for (StateStep<S> step : sequence()) if (step.operation().test(stack)) return step.state();
        if (recipe.hasAlternateBranch())
            for (StateStep<S> step : recipe.sequence(branch.next()))
                if (step.operation().test(stack)) return step.state();
        return null;
    }

    /**
     * The submitted operation belongs to the other branch's layout at this index. Sequences are
     * compared state by state, so this also covers a shorter current sequence whose index is past
     * its own end.
     */
    private boolean isBranchConflict(StateId submitted) {
        if (submitted == null || !recipe.hasAlternateBranch()) return false;
        List<StateStep<S>> other = recipe.sequence(branch.next());
        if (stateIndex >= other.size()) return false;
        StateId here = stateIndex < sequence().size() ? sequence().get(stateIndex).state() : null;
        return other.get(stateIndex).state() == submitted && here != submitted;
    }

    private List<StateStep<S>> sequence() {
        return recipe.sequence(branch);
    }

    private StateStep<S> currentStep() {
        return sequence().get(stateIndex);
    }

    private int branchIndex() {
        return 0;
    }

    private int convergeIndex() {
        for (int i = 0; i < sequence().size(); i++)
            if (sequence().get(i).state() == StateId.CONVERGE) return i;
        throw new IllegalStateException("validated recipe has no converge");
    }

    public ReactorFormula.Result finalResult() {
        if (recipe == null) throw new IllegalStateException("no active recipe");
        return ReactorFormula.calculate(
                recipe.baseFluidCost(),
                recipe.targetOutput(),
                timeReduction,
                timePenalty,
                fluidReductionBp,
                fluidPenaltyBp,
                outputBonusBp,
                outputPenaltyBp);
    }

    public Status status() {
        return status;
    }

    public StructureReactorRecipe<S> recipe() {
        return recipe;
    }

    public StructureReactorRecipe.Branch branch() {
        return branch;
    }

    public int stateTicks() {
        return stateTicks;
    }

    /**
     * Ticks spent on the cycle so far. It keeps counting across state changes, unlike {@link
     * #stateTicks()}, so a display layer can report the ritual's total elapsed time.
     */
    public int elapsedTicks() {
        return elapsedTicks;
    }

    public int stateIndex() {
        return stateIndex;
    }

    /** Outcome of the settled step at {@code index}, or {@link Resolution#NONE} if not settled. */
    public Resolution stateOutcome(int index) {
        return index >= 0 && index < stateOutcomes.size()
                ? stateOutcomes.get(index)
                : Resolution.NONE;
    }

    /** Per-step settlement outcomes, index-aligned with the running sequence (server side). */
    public List<Resolution> stateOutcomes() {
        return List.copyOf(stateOutcomes);
    }

    private void recordOutcome(int index, Resolution resolution) {
        if (resolution == Resolution.NONE) return;
        while (stateOutcomes.size() <= index) stateOutcomes.add(Resolution.NONE);
        stateOutcomes.set(index, resolution);
    }

    public StateId currentState() {
        return status == Status.RUNNING ? currentStep().state() : null;
    }

    public int carriedExtraRecursion() {
        return carriedExtraRecursion;
    }

    public int extraFragmentCost() {
        return extraFragmentCost;
    }

    /** Accumulated rewards of the current cycle, in ticks. */
    public int timeReduction() {
        return timeReduction;
    }

    /** Accumulated time penalties of the current cycle, in ticks. */
    public int timePenalty() {
        return timePenalty;
    }

    public int fluidReductionBp() {
        return fluidReductionBp;
    }

    public int fluidPenaltyBp() {
        return fluidPenaltyBp;
    }

    public int outputBonusBp() {
        return outputBonusBp;
    }

    public int outputPenaltyBp() {
        return outputPenaltyBp;
    }

    public void save(CompoundTag tag) {
        tag.putInt("ReactorBranch", branch.ordinal());
        tag.putInt("ReactorCarriedRecursion", carriedExtraRecursion);
        tag.putBoolean("ReactorCarriedSettled", carriedRecursionAlreadySettled);
        tag.putInt("ReactorStatus", status.ordinal());
        if (recipe != null) tag.putString("ReactorRecipe", recipe.id().toString());
        tag.putInt("ReactorStateIndex", stateIndex);
        tag.putInt("ReactorStateTicks", stateTicks);
        tag.putInt("ReactorElapsedTicks", elapsedTicks);
        tag.putInt("ReactorDepth", currentDepth);
        tag.putInt("ReactorTimeReduction", timeReduction);
        tag.putInt("ReactorTimePenalty", timePenalty);
        tag.putInt("ReactorFluidReduction", fluidReductionBp);
        tag.putInt("ReactorFluidPenalty", fluidPenaltyBp);
        tag.putInt("ReactorOutputBonus", outputBonusBp);
        tag.putInt("ReactorOutputPenalty", outputPenaltyBp);
        tag.putInt("ReactorCurrentExtra", currentCycleExtraRecursion);
        tag.putInt("ReactorPendingCarry", pendingCarriedRecursion);
        tag.putInt("ReactorExtraFragments", extraFragmentCost);
    }

    /**
     * Restores the durable fields. The active recipe is resolved by the caller so the state machine
     * never has to know which stack type the recipe registry holds.
     */
    public void loadPersistent(
            CompoundTag tag,
            java.util.function.Function<ResourceLocation, StructureReactorRecipe<S>> resolver) {
        int index = tag.getInt("ReactorBranch");
        branch =
                index == StructureReactorRecipe.Branch.B.ordinal()
                        ? StructureReactorRecipe.Branch.B
                        : StructureReactorRecipe.Branch.A;
        carriedExtraRecursion =
                Math.min(
                        MAX_CARRIED_EXTRA_RECURSION,
                        Math.max(0, tag.getInt("ReactorCarriedRecursion")));
        carriedRecursionAlreadySettled =
                !tag.contains("ReactorCarriedSettled") || tag.getBoolean("ReactorCarriedSettled");
        int savedStatus = tag.getInt("ReactorStatus");
        String recipeId = tag.getString("ReactorRecipe");
        ResourceLocation id = ResourceLocation.tryParse(recipeId);
        recipe = id == null || resolver == null ? null : resolver.apply(id);
        if (recipe == null || savedStatus == Status.IDLE.ordinal()) {
            status = Status.IDLE;
            return;
        }
        status =
                switch (savedStatus) {
                    case 2 -> Status.REFINING;
                    case 3 -> Status.READY_TO_COMMIT;
                    default -> Status.RUNNING;
                };
        stateIndex = Math.max(0, Math.min(tag.getInt("ReactorStateIndex"), sequence().size()));
        stateTicks = Math.min(STATE_TIMEOUT_TICKS, Math.max(0, tag.getInt("ReactorStateTicks")));
        elapsedTicks = Math.max(0, tag.getInt("ReactorElapsedTicks"));
        currentDepth = Math.max(0, tag.getInt("ReactorDepth"));
        timeReduction = Math.max(0, tag.getInt("ReactorTimeReduction"));
        timePenalty = Math.max(0, tag.getInt("ReactorTimePenalty"));
        fluidReductionBp = Math.max(0, tag.getInt("ReactorFluidReduction"));
        fluidPenaltyBp = Math.max(0, tag.getInt("ReactorFluidPenalty"));
        outputBonusBp = Math.max(0, tag.getInt("ReactorOutputBonus"));
        outputPenaltyBp = Math.max(0, tag.getInt("ReactorOutputPenalty"));
        currentCycleExtraRecursion = Math.max(0, tag.getInt("ReactorCurrentExtra"));
        pendingCarriedRecursion = Math.max(0, tag.getInt("ReactorPendingCarry"));
        if (!carriedRecursionAlreadySettled) {
            // A legacy or interrupted save kept unsettled recursion in the carried slot; move it
            // back into the live counter so this cycle's stabilize may double it exactly once.
            currentCycleExtraRecursion += carriedExtraRecursion;
            carriedExtraRecursion = 0;
            carriedRecursionAlreadySettled = true;
        }
        extraFragmentCost =
                Math.min(
                        recipe.fragmentCount() * MAX_EXTRA_FRAGMENT_MULTIPLIER,
                        Math.max(0, tag.getInt("ReactorExtraFragments")));
    }

    public enum Status {
        IDLE,
        RUNNING,
        REFINING,
        READY_TO_COMMIT
    }

    public enum Resolution {
        NONE(false),
        CORRECT(true),
        CORRECT_REWARDED(true),
        PHASE_IDLE(true),
        EARLY_CONVERGE(true),
        RECURSION_OVERFLOW(true),
        BRANCH_CONFLICT(true),
        STABILIZE_FAILURE(true);
        private final boolean consumesInput;

        Resolution(boolean consumesInput) {
            this.consumesInput = consumesInput;
        }

        public boolean consumesInput() {
            return consumesInput;
        }
    }
}
