package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.structurereactor.ReactorFormula;
import com.suntide_20210418.dimensiontech.structurereactor.StateStep;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads the in-memory reactor registry into JEI display entries. A recipe with an alternate branch
 * becomes two entries, because the two branches carry different ritual sequences.
 *
 * <p>Nothing is filtered out: a recipe whose fluid or operation is absent still produces an entry,
 * so the display layer stays the only place that decides what to draw.
 */
public final class StructureReactorJeiRecipes {
    /** Used when the registry is empty, so an idle page still has a sane shape. */
    public static final int FALLBACK_ROWS = 6;

    /** A page longer than this stops being readable, so extra states are summarised instead. */
    public static final int MAX_ROWS = 12;

    private StructureReactorJeiRecipes() {}

    public static <S> List<StructureReactorJeiRecipe> build(
            Collection<StructureReactorRecipe<S>> recipes) {
        List<StructureReactorJeiRecipe> entries = new ArrayList<>();
        for (StructureReactorRecipe<S> recipe : recipes) {
            boolean alternate = recipe.hasAlternateBranch();
            entries.add(entry(recipe, StructureReactorRecipe.Branch.A, alternate));
            if (alternate) entries.add(entry(recipe, StructureReactorRecipe.Branch.B, true));
        }
        return List.copyOf(entries);
    }

    /** The longest ritual sequence any entry can show, used to size fixed-height recipe pages. */
    public static <S> int longestSequence(Collection<StructureReactorRecipe<S>> recipes) {
        int longest = 0;
        for (StructureReactorRecipe<S> recipe : recipes) {
            longest = Math.max(longest, recipe.sequence(StructureReactorRecipe.Branch.A).size());
            if (recipe.hasAlternateBranch())
                longest =
                        Math.max(longest, recipe.sequence(StructureReactorRecipe.Branch.B).size());
        }
        return longest;
    }

    /**
     * The number of state rows a JEI page reserves. JEI fixes a category's height rather than a
     * recipe's, so this is computed from the whole registry once, at category registration.
     */
    public static int visibleRows(int longestSequence) {
        if (longestSequence <= 0) return FALLBACK_ROWS;
        return Math.min(MAX_ROWS, longestSequence);
    }

    private static <S> StructureReactorJeiRecipe entry(
            StructureReactorRecipe<S> recipe,
            StructureReactorRecipe.Branch branch,
            boolean alternate) {
        List<StructureReactorJeiRecipe.DisplayStep> steps = new ArrayList<>();
        for (StateStep<S> step : recipe.sequence(branch)) {
            steps.add(
                    new StructureReactorJeiRecipe.DisplayStep(
                            step.state(), step.operation().asIngredient().orElse(null)));
        }
        return new StructureReactorJeiRecipe(
                recipe.id(),
                branch,
                alternate,
                recipe.input(),
                recipe.baseFluidCost(),
                recipe.output(),
                recipe.targetOutput(),
                recipe.fragment(),
                recipe.fragmentCount(),
                steps,
                recipe.targetDepth(branch),
                ReactorFormula.bounds(recipe.baseFluidCost(), recipe.targetOutput()),
                recipe.fragmentCount() * StructureReactorCycle.MAX_EXTRA_FRAGMENT_MULTIPLIER);
    }
}
