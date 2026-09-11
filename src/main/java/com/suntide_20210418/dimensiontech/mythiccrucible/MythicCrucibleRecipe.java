package com.suntide_20210418.dimensiontech.mythiccrucible;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

/**
 * Immutable V1 crucible recipe. A recipe has one DSL, except tier five's explicit A/B pair.
 *
 * @param <S> the supplied stack type, an {@code ItemStack} in production
 */
public final class MythicCrucibleRecipe<S> {
    public static final int BASE_TIME_TICKS = 400;
    public static final int DEFAULT_FLUID_COST_MB = 1_000;
    public static final int DEFAULT_OUTPUT_AMOUNT_MB = 1_000;

    private final ResourceLocation id;
    private final Fluid input;
    private final Fluid output;
    private final Ingredient fragment;
    private final int fragmentCount;
    private final int baseFluidCost;
    private final int targetOutput;
    private final List<StateStep<S>> sequenceA;
    private final List<StateStep<S>> sequenceB;

    public MythicCrucibleRecipe(
            ResourceLocation id,
            Fluid input,
            Fluid output,
            Ingredient fragment,
            int fragmentCount,
            int baseFluidCost,
            int targetOutput,
            List<StateStep<S>> sequenceA,
            List<StateStep<S>> sequenceB) {
        this.id = Objects.requireNonNull(id, "id");
        this.input = input;
        this.output = output;
        this.fragment = fragment;
        if (fragment != null && fragment.isEmpty())
            throw new IllegalArgumentException("fragment must not be empty");
        if (fragmentCount < 1 || baseFluidCost < 1 || targetOutput < 1)
            throw new IllegalArgumentException("recipe costs and output must be positive");
        this.fragmentCount = fragmentCount;
        this.baseFluidCost = baseFluidCost;
        this.targetOutput = targetOutput;
        this.sequenceA = validate(sequenceA, "stateSequence");
        this.sequenceB = sequenceB == null ? List.of() : validate(sequenceB, "stateSequenceB");
        if (!this.sequenceB.isEmpty() && !hasSameEndpoints(this.sequenceA, this.sequenceB))
            throw new IllegalArgumentException(
                    "A/B sequences must share branch and stabilize endpoints");
    }

    private static <S> List<StateStep<S>> validate(List<StateStep<S>> source, String name) {
        if (source == null || source.isEmpty())
            throw new IllegalArgumentException(name + " must not be empty");
        List<StateStep<S>> steps = List.copyOf(source);
        if (steps.get(0).state() != StateId.BRANCH)
            throw new IllegalArgumentException(name + " must begin with branch");
        if (steps.get(steps.size() - 1).state() != StateId.STABILIZE)
            throw new IllegalArgumentException(name + " must end with stabilize");
        int converge = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).state() == StateId.CONVERGE) converge = i;
        }
        if (converge < 0 || converge >= steps.size() - 1)
            throw new IllegalArgumentException(name + " requires converge before stabilize");
        return steps;
    }

    private static <S> boolean hasSameEndpoints(List<StateStep<S>> a, List<StateStep<S>> b) {
        return a.get(0).state() == StateId.BRANCH
                && b.get(0).state() == StateId.BRANCH
                && a.get(a.size() - 1).state() == StateId.STABILIZE
                && b.get(b.size() - 1).state() == StateId.STABILIZE;
    }

    public ResourceLocation id() {
        return id;
    }

    public Fluid input() {
        return input;
    }

    public Fluid output() {
        return output;
    }

    public Ingredient fragment() {
        return fragment;
    }

    public int fragmentCount() {
        return fragmentCount;
    }

    public int baseFluidCost() {
        return baseFluidCost;
    }

    public int targetOutput() {
        return targetOutput;
    }

    public boolean hasAlternateBranch() {
        return !sequenceB.isEmpty();
    }

    public List<StateStep<S>> sequence(Branch branch) {
        if (branch != Branch.B || !hasAlternateBranch()) return sequenceA;
        @SuppressWarnings("unchecked")
        List<StateStep<S>> alternate = (List<StateStep<S>>) (List<?>) sequenceB;
        return alternate;
    }

    public int targetDepth(Branch branch) {
        return (int)
                sequence(branch).stream().filter(step -> step.state() == StateId.RECURSE).count();
    }

    public enum Branch {
        A,
        B;

        public Branch next() {
            return this == A ? B : A;
        }
    }
}
