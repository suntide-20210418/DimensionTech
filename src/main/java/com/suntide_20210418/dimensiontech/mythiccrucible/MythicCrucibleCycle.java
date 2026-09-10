package com.suntide_20210418.dimensiontech.mythiccrucible;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side finite-state machine for one crucible. It deliberately owns no inventory or fluid
 * containers: callers reserve/commit resources around this durable state.
 */
public final class MythicCrucibleCycle {
    public static final int STATE_TIMEOUT_TICKS = 100;
    public static final int REWARD_START_TICK = 20;
    public static final int REWARD_END_TICK = 60;
    public static final int MAX_CARRIED_EXTRA_RECURSION = 4;

    private MythicCrucibleRecipe recipe;
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

    public void start(MythicCrucibleRecipe recipe) {
        if (status != Status.IDLE) throw new IllegalStateException("a cycle is already active");
        this.recipe = recipe;
        stateIndex = stateTicks = currentDepth = 0;
        timeReduction = timePenalty = fluidReductionBp = fluidPenaltyBp = outputBonusBp = outputPenaltyBp = 0;
        currentCycleExtraRecursion = pendingCarriedRecursion = extraFragmentCost = 0;
        status = Status.RUNNING;
    }

    /** Advance the waiting timer; callers resolve an inserted item during the same server tick. */
    public void tick() {
        if (status == Status.RUNNING) stateTicks++;
    }

    /** Resolves exactly one supplied operation stack. Empty stacks are intentionally ignored. */
    public Resolution resolve(ItemStack stack) {
        if (status != Status.RUNNING || stack.isEmpty()) return Resolution.NONE;
        if (stateTicks > STATE_TIMEOUT_TICKS) {
            timePenalty += 60;
            stateTicks = 0;
            return Resolution.PHASE_IDLE;
        }
        StateStep expected = currentStep();
        StateId submitted = stateFor(stack);
        if (submitted == expected.state() && expected.operation().test(stack)) return correct();
        if (isBranchConflict(stack)) {
            timePenalty += 80;
            stateIndex = branchIndex();
            stateTicks = 0;
            return Resolution.BRANCH_CONFLICT;
        }
        if (submitted == StateId.CONVERGE && currentDepth < recipe.targetDepth(branch)) {
            outputPenaltyBp += 1_000 * (recipe.targetDepth(branch) - currentDepth);
            stateIndex = convergeIndex() + 1; // the submitted converge settles the convergence point.
            stateTicks = 0;
            return Resolution.EARLY_CONVERGE;
        }
        if (submitted == StateId.RECURSE && currentDepth > recipe.targetDepth(branch)) {
            fluidPenaltyBp += 2_500;
            currentDepth = Math.max(0, recipe.targetDepth(branch) - 1);
            stateTicks = 0;
            return Resolution.RECURSION_OVERFLOW;
        }
        if (submitted == StateId.STABILIZE && expected.state() != StateId.CONVERGE) {
            extraFragmentCost = Math.min(recipe.fragmentCount() * 2, extraFragmentCost + 1);
            stateTicks = 0;
            return Resolution.STABILIZE_FAILURE;
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
        consumeAutomaticRecursion();
        if (stateIndex >= sequence().size()) status = Status.READY_TO_COMMIT;
        return rewarded ? Resolution.CORRECT_REWARDED : Resolution.CORRECT;
    }

    private void settleStabilizeReward() {
        timeReduction *= 2;
        fluidReductionBp *= 2;
        outputBonusBp *= 2;
        currentCycleExtraRecursion *= 2;
        pendingCarriedRecursion *= 2;
    }

    private void consumeAutomaticRecursion() {
        while (stateIndex < sequence().size() && currentStep().state() == StateId.RECURSE) {
            if (currentCycleExtraRecursion > 0) currentCycleExtraRecursion--;
            else if (carriedExtraRecursion > 0) carriedExtraRecursion--;
            else break;
            currentDepth++;
            stateIndex++;
            stateTicks = 0;
        }
        if (stateIndex >= sequence().size() || currentStep().state() != StateId.RECURSE) {
            pendingCarriedRecursion += currentCycleExtraRecursion;
            currentCycleExtraRecursion = 0;
        }
    }

    /** Called exactly after output and resource settlement succeed. */
    public void commit() {
        if (status != Status.READY_TO_COMMIT) throw new IllegalStateException("cycle is not complete");
        carriedExtraRecursion = Math.min(MAX_CARRIED_EXTRA_RECURSION, carriedExtraRecursion + pendingCarriedRecursion);
        carriedRecursionAlreadySettled = true;
        if (recipe.hasAlternateBranch()) branch = branch.next();
        clearCycle();
    }

    /** Roll back an uncommitted cycle. Changing recipes additionally clears persistent rewards/branch. */
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
        timeReduction = timePenalty = fluidReductionBp = fluidPenaltyBp = outputBonusBp = outputPenaltyBp = 0;
        currentCycleExtraRecursion = pendingCarriedRecursion = extraFragmentCost = 0;
    }

    private StateId stateFor(ItemStack stack) {
        for (StateStep step : sequence()) if (step.operation().test(stack)) return step.state();
        if (recipe.hasAlternateBranch())
            for (StateStep step : recipe.sequence(branch.next())) if (step.operation().test(stack)) return step.state();
        return null;
    }

    private boolean isBranchConflict(ItemStack stack) {
        if (!recipe.hasAlternateBranch()) return false;
        List<StateStep> other = recipe.sequence(branch.next());
        return stateIndex < other.size()
                && stateIndex < sequence().size()
                && other.get(stateIndex).operation().test(stack)
                && !currentStep().operation().test(stack);
    }
    private List<StateStep> sequence() { return recipe.sequence(branch); }
    private StateStep currentStep() { return sequence().get(stateIndex); }
    private int branchIndex() { return 0; }
    private int convergeIndex() {
        for (int i = 0; i < sequence().size(); i++) if (sequence().get(i).state() == StateId.CONVERGE) return i;
        throw new IllegalStateException("validated recipe has no converge");
    }

    public CrucibleFormula.Result finalResult() {
        if (recipe == null) throw new IllegalStateException("no active recipe");
        return CrucibleFormula.calculate(recipe.baseFluidCost(), recipe.targetOutput(), timeReduction, timePenalty, fluidReductionBp, fluidPenaltyBp, outputBonusBp, outputPenaltyBp);
    }
    public Status status() { return status; }
    public MythicCrucibleRecipe recipe() { return recipe; }
    public MythicCrucibleRecipe.Branch branch() { return branch; }
    public int stateTicks() { return stateTicks; }
    public int stateIndex() { return stateIndex; }
    public StateId currentState() { return status == Status.RUNNING ? currentStep().state() : null; }
    public int carriedExtraRecursion() { return carriedExtraRecursion; }
    public int extraFragmentCost() { return extraFragmentCost; }

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
    public void loadPersistent(CompoundTag tag) {
        int index = tag.getInt("CrucibleBranch");
        branch = index == MythicCrucibleRecipe.Branch.B.ordinal() ? MythicCrucibleRecipe.Branch.B : MythicCrucibleRecipe.Branch.A;
        carriedExtraRecursion = Math.min(MAX_CARRIED_EXTRA_RECURSION, Math.max(0, tag.getInt("CrucibleCarriedRecursion")));
        carriedRecursionAlreadySettled = !tag.contains("CrucibleCarriedSettled") || tag.getBoolean("CrucibleCarriedSettled");
        int savedStatus = tag.getInt("CrucibleStatus");
        String recipeId = tag.getString("CrucibleRecipe");
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(recipeId);
        recipe = id == null ? null : MythicCrucibleRecipes.get(id);
        if (recipe == null || savedStatus == Status.IDLE.ordinal()) {
            status = Status.IDLE;
            return;
        }
        status = savedStatus == Status.READY_TO_COMMIT.ordinal() ? Status.READY_TO_COMMIT : Status.RUNNING;
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
        extraFragmentCost = Math.min(recipe.fragmentCount() * 2, Math.max(0, tag.getInt("CrucibleExtraFragments")));
    }

    public enum Status { IDLE, RUNNING, READY_TO_COMMIT }
    public enum Resolution {
        NONE(false), CORRECT(true), CORRECT_REWARDED(true), PHASE_IDLE(true), EARLY_CONVERGE(true),
        RECURSION_OVERFLOW(true), BRANCH_CONFLICT(true), STABILIZE_FAILURE(true);
        private final boolean consumesInput;
        Resolution(boolean consumesInput) { this.consumesInput = consumesInput; }
        public boolean consumesInput() { return consumesInput; }
    }
}
