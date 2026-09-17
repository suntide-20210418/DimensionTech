package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleFormula;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleCycle;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipe;
import com.suntide_20210418.dimensiontech.mythiccrucible.StateStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads the in-memory crucible registry into JEI display entries. A recipe with an alternate branch
 * becomes two entries, because the two branches carry different ritual sequences.
 *
 * <p>Nothing is filtered out: a recipe whose fluid or operation is absent still produces an entry,
 * so the display layer stays the only place that decides what to draw.
 */
public final class MythicCrucibleJeiRecipes {
    /** Used when the registry is empty, so an idle page still has a sane shape. */
    public static final int FALLBACK_ROWS = 6;

    /** A page longer than this stops being readable, so extra states are summarised instead. */
    public static final int MAX_ROWS = 12;

    private MythicCrucibleJeiRecipes() {}

    public static <S> List<MythicCrucibleJeiRecipe> build(
            Collection<MythicCrucibleRecipe<S>> recipes) {
        List<MythicCrucibleJeiRecipe> entries = new ArrayList<>();
        for (MythicCrucibleRecipe<S> recipe : recipes) {
            boolean alternate = recipe.hasAlternateBranch();
            entries.add(entry(recipe, MythicCrucibleRecipe.Branch.A, alternate));
            if (alternate) entries.add(entry(recipe, MythicCrucibleRecipe.Branch.B, true));
        }
        return List.copyOf(entries);
    }

    /** The longest ritual sequence any entry can show, used to size fixed-height recipe pages. */
    public static <S> int longestSequence(Collection<MythicCrucibleRecipe<S>> recipes) {
        int longest = 0;
        for (MythicCrucibleRecipe<S> recipe : recipes) {
            longest = Math.max(longest, recipe.sequence(MythicCrucibleRecipe.Branch.A).size());
            if (recipe.hasAlternateBranch())
                longest = Math.max(longest, recipe.sequence(MythicCrucibleRecipe.Branch.B).size());
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

    private static <S> MythicCrucibleJeiRecipe entry(
            MythicCrucibleRecipe<S> recipe, MythicCrucibleRecipe.Branch branch, boolean alternate) {
        List<MythicCrucibleJeiRecipe.DisplayStep> steps = new ArrayList<>();
        for (StateStep<S> step : recipe.sequence(branch)) {
            steps.add(
                    new MythicCrucibleJeiRecipe.DisplayStep(
                            step.state(), step.operation().asIngredient().orElse(null)));
        }
        return new MythicCrucibleJeiRecipe(
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
                CrucibleFormula.bounds(recipe.baseFluidCost(), recipe.targetOutput()),
                recipe.fragmentCount() * MythicCrucibleCycle.MAX_EXTRA_FRAGMENT_MULTIPLIER);
    }
}
