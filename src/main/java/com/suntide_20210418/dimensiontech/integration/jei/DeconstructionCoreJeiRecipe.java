package com.suntide_20210418.dimensiontech.integration.jei;

/**
 * The single entry of the deconstruction-core's independent JEI page.
 *
 * <p>It carries the probability and pity figures the page advertises, mirrored from the miner reward
 * generator (per-tier mine chance) and the chest-loot hook (per-chest chance plus the pity ceiling),
 * so the page can show concrete numbers without needing a running game.
 */
public record DeconstructionCoreJeiRecipe(
        double minMineChancePerRoll,
        double maxMineChancePerRoll,
        double chestDropChance,
        int pityChests) {}