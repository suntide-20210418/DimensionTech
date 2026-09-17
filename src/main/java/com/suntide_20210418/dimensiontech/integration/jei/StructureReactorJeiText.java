package com.suntide_20210418.dimensiontech.integration.jei;

import com.suntide_20210418.dimensiontech.structurereactor.ReactorFormula;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorCycle;
import com.suntide_20210418.dimensiontech.structurereactor.StateId;
import java.util.List;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

/**
 * Every string the reactor JEI category shows, and the arithmetic behind the numbers in them.
 *
 * <p>All reward and penalty figures are read from {@link StructureReactorCycle} and {@link
 * ReactorFormula}, so the page can never advertise a rule the state machine does not implement.
 */
final class StructureReactorJeiText {
    static final String TITLE = "jei.dimension_tech.structure_reactor.title";

    static final String IO = "jei.dimension_tech.structure_reactor.io";
    static final String BRANCH = "jei.dimension_tech.structure_reactor.branch";
    static final String BRANCH_ALTERNATE = "jei.dimension_tech.structure_reactor.branch.alternate";
    static final String SEQUENCE = "jei.dimension_tech.structure_reactor.sequence";
    static final String ROW = "jei.dimension_tech.structure_reactor.row";
    static final String ROW_UNKNOWN = "jei.dimension_tech.structure_reactor.row.unknown";
    static final String MORE = "jei.dimension_tech.structure_reactor.more";

    static final String STEP = "jei.dimension_tech.structure_reactor.step";
    static final String STEP_REQUIRES = "jei.dimension_tech.structure_reactor.step.requires";
    static final String STEP_REQUIRES_UNKNOWN =
            "jei.dimension_tech.structure_reactor.step.requires.unknown";
    static final String STEP_TIMEOUT = "jei.dimension_tech.structure_reactor.step.timeout";

    static final String REWARD_WINDOW = "jei.dimension_tech.structure_reactor.reward.window";
    static final String REWARD_BRANCH = "jei.dimension_tech.structure_reactor.reward.branch";
    static final String REWARD_RECURSE = "jei.dimension_tech.structure_reactor.reward.recurse";
    static final String REWARD_CONVERGE = "jei.dimension_tech.structure_reactor.reward.converge";
    static final String REWARD_STABILIZE = "jei.dimension_tech.structure_reactor.reward.stabilize";

    static final String PENALTY_STABILIZE = "jei.dimension_tech.structure_reactor.penalty.stabilize";
    static final String PENALTY_CONVERGE = "jei.dimension_tech.structure_reactor.penalty.converge";
    static final String PENALTY_RECURSE = "jei.dimension_tech.structure_reactor.penalty.recurse";
    static final String PENALTY_BRANCH = "jei.dimension_tech.structure_reactor.penalty.branch";

    static final String RANGE_TIME = "jei.dimension_tech.structure_reactor.range.time";
    static final String RANGE_FLUID = "jei.dimension_tech.structure_reactor.range.fluid";
    static final String RANGE_OUTPUT = "jei.dimension_tech.structure_reactor.range.output";
    static final String RANGE_FRAGMENTS = "jei.dimension_tech.structure_reactor.range.fragments";

    private StructureReactorJeiText() {}

    /** Reuses the reactor screen's own stage wording so both surfaces read identically. */
    static Component stageName(StateId state) {
        String key =
                switch (state) {
                    case BRANCH -> "screen.dimension_tech.structure_reactor.stage.branch";
                    case RECURSE -> "screen.dimension_tech.structure_reactor.stage.recurse";
                    case CONVERGE -> "screen.dimension_tech.structure_reactor.stage.converge";
                    case STABILIZE -> "screen.dimension_tech.structure_reactor.stage.stabilize";
                };
        return Component.translatable(key);
    }

    /** The operation item's display name, or the dedicated unknown label for predicate recipes. */
    static Component operationName(@Nullable Ingredient operation) {
        if (operation == null) return Component.translatable(ROW_UNKNOWN);
        ItemStack[] items = operation.getItems();
        if (items.length == 0 || items[0].isEmpty()) return Component.translatable(ROW_UNKNOWN);
        return items[0].getHoverName();
    }

    static Component ioLine(StructureReactorJeiRecipe recipe) {
        return Component.translatable(IO, recipe.inputAmountMb(), recipe.outputAmountMb());
    }

    static Component branchLine(StructureReactorJeiRecipe recipe) {
        return Component.translatable(BRANCH, recipe.branch().name(), recipe.targetDepth());
    }

    /**
     * Only rendered for recipes that really alternate, so the reserved line stays blank otherwise.
     */
    static Component alternateLine() {
        return Component.translatable(BRANCH_ALTERNATE);
    }

    static Component rowLine(StructureReactorJeiRecipe.DisplayStep step) {
        return Component.translatable(
                ROW, stageName(step.state()), operationName(step.operation()));
    }

    static Component moreLine(int hidden) {
        return Component.translatable(MORE, hidden);
    }

    static List<FormattedText> footerLines(StructureReactorJeiRecipe recipe) {
        ReactorFormula.Result min = recipe.bounds().min();
        ReactorFormula.Result max = recipe.bounds().max();
        return List.of(
                Component.translatable(
                        REWARD_WINDOW,
                        StructureReactorCycle.REWARD_START_TICK,
                        StructureReactorCycle.REWARD_END_TICK),
                Component.translatable(RANGE_TIME, min.timeTicks(), max.timeTicks()),
                Component.translatable(RANGE_FLUID, min.fluidCostMb(), max.fluidCostMb()),
                Component.translatable(RANGE_OUTPUT, min.outputAmountMb(), max.outputAmountMb()),
                Component.translatable(
                        RANGE_FRAGMENTS, recipe.fragmentCount(), recipe.maxExtraFragments()));
    }

    /**
     * The full rule text for one ritual state: what it wants, what a settlement inside the reward
     * window pays, and what a wrong submission costs.
     */
    static void appendStepTooltip(
            ITooltipBuilder tooltip, StructureReactorJeiRecipe.DisplayStep step, int index) {
        tooltip.add(Component.translatable(STEP, index, stageName(step.state())));
        tooltip.add(
                step.operation() == null
                        ? Component.translatable(STEP_REQUIRES_UNKNOWN)
                        : Component.translatable(STEP_REQUIRES, operationName(step.operation())));
        tooltip.add(rewardLine(step.state()));
        tooltip.add(penaltyLine(step.state()));
        tooltip.add(
                Component.translatable(
                        STEP_TIMEOUT,
                        StructureReactorCycle.STATE_TIMEOUT_TICKS,
                        StructureReactorCycle.WRONG_STATE_TIME_PENALTY_TICKS));
    }

    private static Component rewardLine(StateId state) {
        return switch (state) {
            case BRANCH ->
                    Component.translatable(
                            REWARD_BRANCH, StructureReactorCycle.REWARD_BRANCH_TIME_REDUCTION_TICKS);
            case RECURSE ->
                    Component.translatable(
                            REWARD_RECURSE,
                            percent(StructureReactorCycle.REWARD_RECURSE_FLUID_REDUCTION_BP));
            case CONVERGE ->
                    Component.translatable(
                            REWARD_CONVERGE,
                            percent(StructureReactorCycle.REWARD_CONVERGE_OUTPUT_BONUS_BP));
            case STABILIZE -> Component.translatable(REWARD_STABILIZE);
        };
    }

    private static Component penaltyLine(StateId state) {
        return switch (state) {
            case BRANCH ->
                    Component.translatable(
                            PENALTY_BRANCH, StructureReactorCycle.BRANCH_CONFLICT_TIME_PENALTY_TICKS);
            case RECURSE ->
                    Component.translatable(
                            PENALTY_RECURSE,
                            percent(StructureReactorCycle.RECURSION_OVERFLOW_FLUID_PENALTY_BP));
            case CONVERGE ->
                    Component.translatable(
                            PENALTY_CONVERGE,
                            percent(
                                    StructureReactorCycle
                                            .EARLY_CONVERGE_OUTPUT_PENALTY_BP_PER_DEPTH));
            case STABILIZE ->
                    Component.translatable(
                            PENALTY_STABILIZE,
                            StructureReactorCycle.STABILIZE_FAILURE_EXTRA_FRAGMENTS,
                            StructureReactorCycle.MAX_EXTRA_FRAGMENT_MULTIPLIER);
        };
    }

    private static int percent(int basisPoints) {
        return ReactorFormula.percent(basisPoints);
    }
}
