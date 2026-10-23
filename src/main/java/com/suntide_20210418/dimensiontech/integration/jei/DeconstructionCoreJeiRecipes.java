package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.loot.DimensionCoreChestLoot;
import java.util.List;

/**
 * Builds the single entry of the deconstruction-core page from the reward generator's and the
 * chest-loot hook's constants.
 */
public final class DeconstructionCoreJeiRecipes {
    private DeconstructionCoreJeiRecipes() {}

    public static List<DeconstructionCoreJeiRecipe> build() {
        return List.of(
                new DeconstructionCoreJeiRecipe(
                        StructureMinerJeiRecipes.CORE_CHANCE_PER_TIER,
                        Math.min(1.0D, StructureMinerJeiRecipes.CORE_CHANCE_PER_TIER * 6),
                        DimensionCoreChestLoot.DROP_CHANCE,
                        DimensionCoreChestLoot.PITY_CHEST));
    }
}
