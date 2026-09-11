package com.suntide_20210418.dimensiontech.mythiccrucible;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-side finite-state machine for one crucible. It deliberately owns no inventory or fluid
 * containers: callers reserve/commit resources around this durable state.
 */
public final class MythicCrucibleCycle<S> {
    public static final int STATE_TIMEOUT_TICKS = 100;
    public static final int REWARD_START_TICK = 20;
    public static final int REWARD_END_TICK = 60;
    public static final int MAX_CARRIED_EXTRA_RECURSION = 4;

    private MythicCrucibleRecipe<S> recipe;
    private MythicCrucibleRecipe.Branch branch = MythicCrucibleRecipe.Branch.A;
    private int stateIndex;
    private int stateTicks;
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
    private Status status = Status.IDLE;

    /** Starts a cycle that keeps the branch persisted on this state machine. */
    public void start(MythicCrucibleRecipe<S> recipe) {
        start(recipe, branch);
    }

    /**
     * Starts a cycle on an explicit branch. Tier five alternates its DSL every completed cycle, so
     * callers may restore a persisted branch before starting.
     */
    public void start(MythicCrucibleRecipe<S> recipe, MythicCrucibleRecipe.Branch startBranch) {
        if (status != Status.IDLE) throw new IllegalStateException("a cycle is already active");
        if (recipe.hasAlternateBranch()) {
            if (startBranch == null) throw new IllegalArgumentException("branch must not be null");
            branch = startBranch;
        } else {
            branch = MythicCrucibleRecipe.Branch.A;
        }
        this.recipe = recipe;
        stateIndex = stateTicks = currentDepth = 0;
        timeReduction =
                timePenalty =
                        fluidReductionBp = fluidPenaltyBp = outputBonusBp = outputPenaltyBp = 0;
        currentCycleExtraRecursion = pendingCarriedRecursion = extraFragmentCost = 0;
        status = Status.RUNNING;
    }

    /** Advance the waiting timer; callers resolve an inserted item during the same server tick. */
    public void tick() {
        if (status == Status.RUNNING) stateTicks++;
    }

    /** Resolves a possibly empty supplied stack. */
    public Resolution resolve(S stack) {
        return resolve(stack, stack == null);
    }

    /** Resolves exactly one supplied operation stack. Empty stacks are intentionally ignored. */
    public Resolution resolve(S stack, boolean inputEmpty) {
        if (status != Status.RUNNING || inputEmpty) return Resolution.NONE;
        if (stateTicks > STATE_TIMEOUT_TICKS) {
            timePenalty += 60;
            stateTicks = 0;
            return Resolution.PHASE_IDLE;
        }
        StateStep<S> expected = currentStep();
        StateId expectedState = expected.state();
        StateId submitted = stateFor(stack);
        if (submitted == expectedState && expected.operation().test(stack)) return correct();
        if (submitted == null) {
            timePenalty += 60;
            stateTicks = 0;
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
                                Math.min(recipe.fragmentCount() * 2, extraFragmentCost + 1);
                        stateTicks = 0;
                        return Resolution.STABILIZE_FAILURE;
                    }
                }
            }
            case CONVERGE -> {
                if (currentDepth < targetDepth) {
                    outputPenaltyBp += 1_000 * (targetDepth - currentDepth);
                    switch (expectedState) {
                        case CONVERGE -> stateIndex++;
                        case BRANCH, RECURSE, STABILIZE -> stateIndex = convergeIndex() + 1;
                    }
                    stateTicks = 0;
                    settleStep();
                    return Resolution.EARLY_CONVERGE;
                }
            }
            case RECURSE -> {
                if (currentDepth > targetDepth) {
                    fluidPenaltyBp += 2_500;
                    currentDepth = Math.max(0, targetDepth - 1);
                    stateTicks = 0;
                    return Resolution.RECURSION_OVERFLOW;
                }
            }
            case BRANCH -> {}
        }
        if (isBranchConflict(submitted)) {
            timePenalty += 80;
            stateIndex = branchIndex();
            stateTicks = 0;
            return Resolution.BRANCH_CONFLICT;
        }
        timePenalty += 60;
        stateTicks = 0;
        return Resolution.PHASE_IDLE;
    }

    private Resolution correct() {
        boolean rewarded = stateTicks >= REWARD_START_TICK && stateTicks <= REWARD_END_TICK;
        StateId id = currentStep().state();
        if (rewarded) {
            switch (id) {
                case BRANCH -> timeReduction += 40;
                case RECURSE -> {
                    fluidReductionBp += 1_000;
                    currentCycleExtraRecursion++;
                }
                case CONVERGE -> outputBonusBp += 2_500;
                case STABILIZE -> settleStabilizeReward();
            }
        }
        if (id == StateId.RECURSE) currentDepth++;
        stateIndex++;
        stateTicks = 0;
        settleStep();
        if (stateIndex >= sequence().size()) status = Status.READY_TO_COMMIT;
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
            status = Status.READY_TO_COMMIT;
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
            status = Status.READY_TO_COMMIT;
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
            branch = MythicCrucibleRecipe.Branch.A;
        }
    }

    private void clearCycle() {
        recipe = null;
        status = Status.IDLE;
        stateIndex = stateTicks = currentDepth = 0;
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

    public CrucibleFormula.Result finalResult() {
        if (recipe == null) throw new IllegalStateException("no active recipe");
        return CrucibleFormula.calculate(
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

    public MythicCrucibleRecipe<S> recipe() {
        return recipe;
    }

    public MythicCrucibleRecipe.Branch branch() {
        return branch;
    }

    public int stateTicks() {
        return stateTicks;
    }

    public int stateIndex() {
        return stateIndex;
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

    public void save(CompoundTag tag) {
        tag.putInt("CrucibleBranch", branch.ordinal());
        tag.putInt("CrucibleCarriedRecursion", carriedExtraRecursion);
        tag.putBoolean("CrucibleCarriedSettled", carriedRecursionAlreadySettled);
        tag.putInt("CrucibleStatus", status.ordinal());
        if (recipe != null) tag.putString("CrucibleRecipe", recipe.id().toString());
        tag.putInt("CrucibleStateIndex", stateIndex);
        tag.putInt("CrucibleStateTicks", stateTicks);
        tag.putInt("CrucibleDepth", currentDepth);
        tag.putInt("CrucibleTimeReduction", timeReduction);
        tag.putInt("CrucibleTimePenalty", timePenalty);
        tag.putInt("CrucibleFluidReduction", fluidReductionBp);
        tag.putInt("CrucibleFluidPenalty", fluidPenaltyBp);
        tag.putInt("CrucibleOutputBonus", outputBonusBp);
        tag.putInt("CrucibleOutputPenalty", outputPenaltyBp);
        tag.putInt("CrucibleCurrentExtra", currentCycleExtraRecursion);
        tag.putInt("CruciblePendingCarry", pendingCarriedRecursion);
        tag.putInt("CrucibleExtraFragments", extraFragmentCost);
    }

    /**
     * Restores the durable fields. The active recipe is resolved by the caller so the state machine
     * never has to know which stack type the recipe registry holds.
     */
    public void loadPersistent(
            CompoundTag tag,
            java.util.function.Function<ResourceLocation, MythicCrucibleRecipe<S>> resolver) {
        int index = tag.getInt("CrucibleBranch");
        branch =
                index == MythicCrucibleRecipe.Branch.B.ordinal()
                        ? MythicCrucibleRecipe.Branch.B
                        : MythicCrucibleRecipe.Branch.A;
        carriedExtraRecursion =
                Math.min(
                        MAX_CARRIED_EXTRA_RECURSION,
                        Math.max(0, tag.getInt("CrucibleCarriedRecursion")));
        carriedRecursionAlreadySettled =
                !tag.contains("CrucibleCarriedSettled") || tag.getBoolean("CrucibleCarriedSettled");
        int savedStatus = tag.getInt("CrucibleStatus");
        String recipeId = tag.getString("CrucibleRecipe");
        ResourceLocation id = ResourceLocation.tryParse(recipeId);
        recipe = id == null || resolver == null ? null : resolver.apply(id);
        if (recipe == null || savedStatus == Status.IDLE.ordinal()) {
            status = Status.IDLE;
            return;
        }
        status =
                savedStatus == Status.READY_TO_COMMIT.ordinal()
                        ? Status.READY_TO_COMMIT
                        : Status.RUNNING;
        stateIndex = Math.max(0, Math.min(tag.getInt("CrucibleStateIndex"), sequence().size() - 1));
        stateTicks = Math.max(0, tag.getInt("CrucibleStateTicks"));
        currentDepth = Math.max(0, tag.getInt("CrucibleDepth"));
        timeReduction = Math.max(0, tag.getInt("CrucibleTimeReduction"));
        timePenalty = Math.max(0, tag.getInt("CrucibleTimePenalty"));
        fluidReductionBp = Math.max(0, tag.getInt("CrucibleFluidReduction"));
        fluidPenaltyBp = Math.max(0, tag.getInt("CrucibleFluidPenalty"));
        outputBonusBp = Math.max(0, tag.getInt("CrucibleOutputBonus"));
        outputPenaltyBp = Math.max(0, tag.getInt("CrucibleOutputPenalty"));
        currentCycleExtraRecursion = Math.max(0, tag.getInt("CrucibleCurrentExtra"));
        pendingCarriedRecursion = Math.max(0, tag.getInt("CruciblePendingCarry"));
        if (!carriedRecursionAlreadySettled) {
            // A legacy or interrupted save kept unsettled recursion in the carried slot; move it
            // back into the live counter so this cycle's stabilize may double it exactly once.
            currentCycleExtraRecursion += carriedExtraRecursion;
            carriedExtraRecursion = 0;
            carriedRecursionAlreadySettled = true;
        }
        extraFragmentCost =
                Math.min(
                        recipe.fragmentCount() * 2,
                        Math.max(0, tag.getInt("CrucibleExtraFragments")));
    }

    public enum Status {
        IDLE,
        RUNNING,
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
