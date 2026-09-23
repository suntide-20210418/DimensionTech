package com.suntide_20210418.dimensiontech.integration.jei;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * One displayable miner tier: the essence it consumes per work cycle and the per-cycle product it
 * yields (the tier's own fragments, tokens and the dimension-deconstruction core chance).
 *
 * <p>It holds only registries-backed display values and no game-booting side effects, so the adapter
 * that builds it stays unit-testable without a running game. The counts shown are the cap under the
 * tier's configured base parallel, because the real {@code parallel} is dynamic (upgrades and
 * external acceleration); the category says so in its footer.
 *
 * <p>{@code chestVariant} marks the same tier recipe as fed by a chest marker instead of a structure
 * marker: the input slot and the loot-output slot render differently, everything else is identical.
 * Both variants share one miner page so the player sees two entries in the same category.
 */
public record StructureMinerJeiRecipe(
        int tier,
        @Nullable Fluid inputFluid,
        int fluidPerCycleMb,
        int tankCapacityMb,
        Ingredient outputFragment,
        Ingredient outputToken,
        int outputCount,
        int coreRolls,
        double coreChancePerRoll,
        boolean chestVariant) {
    public StructureMinerJeiRecipe(
            int tier,
            @Nullable Fluid inputFluid,
            int fluidPerCycleMb,
            int tankCapacityMb,
            Ingredient outputFragment,
            Ingredient outputToken,
            int outputCount,
            int coreRolls,
            double coreChancePerRoll) {
        this(
                tier,
                inputFluid,
                fluidPerCycleMb,
                tankCapacityMb,
                outputFragment,
                outputToken,
                outputCount,
                coreRolls,
                coreChancePerRoll,
                false);
    }

    /** The same tier recipe as a chest-marker entry, sharing every output but the marker's profile. */
    public StructureMinerJeiRecipe asChestVariant() {
        return new StructureMinerJeiRecipe(
                tier,
                inputFluid,
                fluidPerCycleMb,
                tankCapacityMb,
                outputFragment,
                outputToken,
                outputCount,
                coreRolls,
                coreChancePerRoll,
                true);
    }
}
