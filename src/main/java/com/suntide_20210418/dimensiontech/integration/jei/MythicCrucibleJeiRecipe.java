package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.mythiccrucible.CrucibleFormula;
import com.suntide_20210418.dimensiontech.mythiccrucible.MythicCrucibleRecipe;
import com.suntide_20210418.dimensiontech.mythiccrucible.StateId;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * One displayable crucible conversion: a recipe read on one branch. It carries only registries-free
 * values so the adapter that builds it stays unit-testable without a running game.
 *
 * <p>{@code input}, {@code output} and {@code fragment} are nullable because the recipe model is
 * generic over its supplied stack and its tests legitimately build recipes without them; the JEI
 * category simply omits those slots.
 */
public record MythicCrucibleJeiRecipe(
        ResourceLocation id,
        MythicCrucibleRecipe.Branch branch,
        boolean alternateBranches,
        @Nullable Fluid input,
        int inputAmountMb,
        @Nullable Fluid output,
        int outputAmountMb,
        @Nullable Ingredient fragment,
        int fragmentCount,
        List<DisplayStep> steps,
        int targetDepth,
        CrucibleFormula.Bounds bounds,
        int maxExtraFragments) {

    public MythicCrucibleJeiRecipe {
        steps = List.copyOf(steps);
    }

    /**
     * One ritual state and the operation item that settles it. {@code operation} is null when the
     * recipe was built from a plain predicate instead of an ingredient.
     */
    public record DisplayStep(StateId state, @Nullable Ingredient operation) {}
}
