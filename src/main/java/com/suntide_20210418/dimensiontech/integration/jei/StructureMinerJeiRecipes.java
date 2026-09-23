package com.suntide_20210418.dimensiontech.integration.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

/**
 * Builds one JEI entry per miner tier.
 *
 * <p>Everything game-live (the tier registry, config parallels, fluid and item registries) is passed
 * in as functional inputs so the adapter stays a plain, registries-free unit like the structure
 * reactor's {@link StructureReactorJeiRecipes}. The two magic numbers here mirror {@code
 * ExpectationRewardGenerator}: at most ten products per cycle and a 0.05 core chance per tier.
 */
public final class StructureMinerJeiRecipes {
    /** Reward output per work cycle is capped at this many items, matching the reward generator. */
    public static final int OUTPUT_COUNT_CAP = 10;

    /** Core drop chance per tier per roll; caps at 1.0 at the top tier. */
    public static final double CORE_CHANCE_PER_TIER = 0.05D;

    private StructureMinerJeiRecipes() {}

    public static List<StructureMinerJeiRecipe> build(
            int fluidPerCycleMb,
            int tankCapacityMb,
            int[] baseParallelByTier,
            IntFunction<Fluid> fluidForTier,
            IntFunction<Ingredient> fragmentForTier,
            IntFunction<Ingredient> tokenForTier) {
        List<StructureMinerJeiRecipe> entries = new ArrayList<>();
        for (int index = 0; index < baseParallelByTier.length; index++) {
            int tier = index + 1;
            int outputCount = Math.min(OUTPUT_COUNT_CAP, Math.max(1, baseParallelByTier[index]));
            entries.add(
                    new StructureMinerJeiRecipe(
                            tier,
                            fluidForTier.apply(tier),
                            fluidPerCycleMb,
                            tankCapacityMb,
                            fragmentForTier.apply(tier),
                            tokenForTier.apply(tier),
                            outputCount,
                            outputCount,
                            Math.min(1.0D, CORE_CHANCE_PER_TIER * tier)));
        }
        return List.copyOf(entries);
    }
}