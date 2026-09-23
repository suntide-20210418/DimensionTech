package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.loot.DimensionCoreChestLoot;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

/**
 * Every string the miner and deconstruction-core JEI categories show, and the arithmetic behind the
 * numbers in them.
 *
 * <p>The miner page carries no body text: each slot's explanation lives in that slot's tooltip. The
 * deconstruction-core page renders its explanation as wrapped text directly on the page.
 */
final class StructureMinerJeiText {
    // Miner page: category title uses the tier's own block name, so no key is needed here.

    // Slot display names.
    static final String MARKER = "jei.dimension_tech.structure_miner.marker";
    static final String LOOT = "jei.dimension_tech.structure_miner.loot";

    // Miner slot tooltips.
    static final String MARKER_TIP = "jei.dimension_tech.structure_miner.marker.tip";
    static final String LOOT_TIP = "jei.dimension_tech.structure_miner.loot.tip";
    static final String PRODUCT = "jei.dimension_tech.structure_miner.product";
    static final String REWARD_NOTE = "jei.dimension_tech.structure_miner.reward.note";
    static final String NOT_CONSUMED = "jei.dimension_tech.structure_miner.not_consumed";

    // Deconstruction-core tooltip on the miner page.
    static final String CORE_CHANCE = "jei.dimension_tech.structure_miner.core.chance";
    static final String CORE_AT_LEAST_ONE = "jei.dimension_tech.structure_miner.core.at_least_one";
    static final String CORE_PITY = "jei.dimension_tech.structure_miner.core.pity";

    // Arrow tooltip: structure value -> processing time.
    static final String TIME_STEP = "jei.dimension_tech.structure_miner.time.step";
    static final String TIME_EFFICIENCY = "jei.dimension_tech.structure_miner.time.efficiency";
    static final String TIME_MINIMUM = "jei.dimension_tech.structure_miner.time.minimum";

    // Deconstruction-core independent page.
    static final String CORE_LINE_CHANCE = "jei.dimension_tech.deconstruction_core.line.chance";
    static final String CORE_LINE_FIRST = "jei.dimension_tech.deconstruction_core.line.first";
    static final String CORE_LINE_USE = "jei.dimension_tech.deconstruction_core.line.use";

    private StructureMinerJeiText() {}

    static Component markerName() {
        return Component.translatable(MARKER);
    }

    static Component markerTip() {
        return Component.translatable(MARKER_TIP);
    }

    /** The label both marker slots carry: the input marker is not consumed by a work cycle. */
    static Component notConsumed() {
        return Component.translatable(NOT_CONSUMED);
    }

    static Component lootName() {
        return Component.translatable(LOOT);
    }

    static Component lootTip() {
        return Component.translatable(LOOT_TIP);
    }

    /** The per-cycle count tooltip shared by the tier's fragments and tokens. */
    static Component productTip(StructureMinerJeiRecipe recipe) {
        return Component.translatable(PRODUCT, recipe.outputCount());
    }

    /** The cap note appended to the structure-loot tooltip. */
    static Component rewardNote() {
        return Component.translatable(REWARD_NOTE);
    }

    // --- deconstruction-core tooltip (miner page core slot) -------------------

    /**
     * The core slot's tooltip on a miner page: the per-cycle rolls, per-roll chance and the mine
     * probability of at least one core. The chest pity stays on the core's own page.
     */
    static List<Component> coreChanceTip(StructureMinerJeiRecipe recipe) {
        return List.of(
                chanceLine(recipe),
                atLeastOneLine(recipe));
    }

    private static Component chanceLine(StructureMinerJeiRecipe recipe) {
        return Component.translatable(
                CORE_CHANCE, recipe.coreRolls(), percent(recipe.coreChancePerRoll()));
    }

    /** {@code 1 - (1 - </code>perRoll<code>)^rolls</code>}, as a rounded percent. */
    private static Component atLeastOneLine(StructureMinerJeiRecipe recipe) {
        double none = Math.pow(1.0D - recipe.coreChancePerRoll(), Math.max(1, recipe.coreRolls()));
        return Component.translatable(CORE_AT_LEAST_ONE, percent(1.0D - none));
    }

    /** The chest-loot pity, guaranteed within {@link DimensionCoreChestLoot#PITY_CHEST} chests. */
    static Component pityLine() {
        return Component.translatable(
                CORE_PITY,
                percent(DimensionCoreChestLoot.DROP_CHANCE),
                DimensionCoreChestLoot.PITY_CHEST);
    }

    // --- arrow tooltip (structure value -> processing time) -------------------

    /**
     * The time-consumption algorithm, from structure value to the work-cycle duration. Only the
     * value-to-ticks steps are stated, as {@code ProcessingMath.processingPlan} computes them.
     */
    static List<Component> timeLines(int minimumTicks) {
        return List.of(
                Component.translatable(TIME_STEP),
                Component.translatable(TIME_EFFICIENCY),
                Component.translatable(TIME_MINIMUM, minimumTicks));
    }

    // --- deconstruction-core independent page ---------------------------------

    /**
     * The source page's wrapped body: the mining-drop chance range, the chest pity, the first
     * acquisition path and the craft it feeds.
     */
    static List<FormattedText> sourcePageLines(DeconstructionCoreJeiRecipe recipe) {
        return List.of(
                coreSourceChanceLine(recipe.minMineChancePerRoll(), recipe.maxMineChancePerRoll()),
                pityLine(),
                firstLine(),
                useLine());
    }

    /** The mining-drop chance range across miner tiers. */
    private static Component coreSourceChanceLine(double minChance, double maxChance) {
        return Component.translatable(
                CORE_LINE_CHANCE, percent(minChance), percent(maxChance));
    }

    private static Component firstLine() {
        return Component.translatable(CORE_LINE_FIRST);
    }

    private static Component useLine() {
        return Component.translatable(CORE_LINE_USE);
    }

    /** Formats a 0..1 chance as a whole-number percent ({@code 0.30 -> 30}). */
    private static int percent(double chance) {
        return (int) Math.round(Math.min(1.0D, Math.max(0.0D, chance)) * 100.0D);
    }
}