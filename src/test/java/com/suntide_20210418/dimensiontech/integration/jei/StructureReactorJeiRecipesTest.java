package com.suntide_20210418.dimensiontech.integration.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.structurereactor.ReactorFormula;
import com.suntide_20210418.dimensiontech.structurereactor.StructureReactorRecipe;
import com.suntide_20210418.dimensiontech.structurereactor.OperationMatcher;
import com.suntide_20210418.dimensiontech.structurereactor.StateId;
import com.suntide_20210418.dimensiontech.structurereactor.StateStep;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * The adapter that turns reactor recipes into JEI entries. The recipes are driven with named
 * tokens and no fluids, exactly like the state machine's own tests, so this stays free of game
 * registries.
 */
class StructureReactorJeiRecipesTest {
    private static final int BASE_FLUID = 1_000;
    private static final int TARGET_OUTPUT = 1_000;

    private static final List<StateStep<String>> SHALLOW =
            sequence(StateId.BRANCH, StateId.CONVERGE, StateId.STABILIZE);

    private static final List<StateStep<String>> DEEP =
            sequence(
                    StateId.BRANCH,
                    StateId.RECURSE,
                    StateId.RECURSE,
                    StateId.CONVERGE,
                    StateId.STABILIZE);

    private static final List<StateStep<String>> ALTERNATE =
            sequence(
                    StateId.BRANCH,
                    StateId.RECURSE,
                    StateId.BRANCH,
                    StateId.RECURSE,
                    StateId.CONVERGE,
                    StateId.STABILIZE);

    @Test
    void aSingleSequenceRecipeBecomesOneEntry() {
        List<StructureReactorJeiRecipe> entries =
                StructureReactorJeiRecipes.build(List.of(recipe("shallow", SHALLOW, null)));

        assertEquals(1, entries.size());
        StructureReactorJeiRecipe entry = entries.get(0);
        assertEquals(StructureReactorRecipe.Branch.A, entry.branch());
        assertFalse(entry.alternateBranches());
        assertEquals(
                List.of(StateId.BRANCH, StateId.CONVERGE, StateId.STABILIZE),
                entry.steps().stream()
                        .map(StructureReactorJeiRecipe.DisplayStep::state)
                        .toList());
        assertEquals(0, entry.targetDepth(), "a shallow sequence recurses zero times");
    }

    @Test
    void anAlternateBranchRecipeBecomesOneEntryPerBranchInOrder() {
        List<StructureReactorJeiRecipe> entries =
                StructureReactorJeiRecipes.build(List.of(recipe("tier_five", DEEP, ALTERNATE)));

        assertEquals(2, entries.size(), "each branch carries its own ritual");
        assertEquals(StructureReactorRecipe.Branch.A, entries.get(0).branch());
        assertEquals(StructureReactorRecipe.Branch.B, entries.get(1).branch());
        assertTrue(entries.get(0).alternateBranches());
        assertTrue(entries.get(1).alternateBranches());
        assertEquals(DEEP.size(), entries.get(0).steps().size());
        assertEquals(ALTERNATE.size(), entries.get(1).steps().size());
        assertEquals(2, entries.get(0).targetDepth());
        assertEquals(
                2,
                entries.get(1).targetDepth(),
                "a nested branch token changes the layout, not the recursion requirement");
    }

    @Test
    void entriesKeepRegistryOrder() {
        List<StructureReactorJeiRecipe> entries =
                StructureReactorJeiRecipes.build(
                        List.of(
                                recipe("first", SHALLOW, null),
                                recipe("second", DEEP, ALTERNATE),
                                recipe("third", SHALLOW, null)));

        assertEquals(4, entries.size(), "two single-branch recipes and one A/B pair");
        assertEquals("first", entries.get(0).id().getPath());
        assertEquals("second", entries.get(1).id().getPath());
        assertEquals("second", entries.get(2).id().getPath());
        assertEquals("third", entries.get(3).id().getPath());
    }

    @Test
    void absentFluidsAndOperationsDoNotDropARecipe() {
        StructureReactorJeiRecipe entry =
                StructureReactorJeiRecipes.build(List.of(recipe("shallow", SHALLOW, null))).get(0);

        assertNull(entry.input());
        assertNull(entry.output());
        for (StructureReactorJeiRecipe.DisplayStep step : entry.steps()) {
            assertNull(step.operation(), "a predicate operation has no ingredient to show");
        }
        assertTrue(entry.steps().size() > 0);
    }

    @Test
    void recipeCostsBecomeTheSettlementBounds() {
        StructureReactorJeiRecipe entry =
                StructureReactorJeiRecipes.build(List.of(recipe("shallow", SHALLOW, null))).get(0);

        ReactorFormula.Bounds expected =
                ReactorFormula.bounds(BASE_FLUID, TARGET_OUTPUT);
        assertEquals(expected, entry.bounds());
        assertEquals(100, entry.bounds().min().timeTicks());
        assertEquals(800, entry.bounds().max().timeTicks());
        assertEquals(250, entry.bounds().min().fluidCostMb());
        assertEquals(2_000, entry.bounds().max().fluidCostMb());
        assertEquals(500, entry.bounds().min().outputAmountMb());
        assertEquals(4_000, entry.bounds().max().outputAmountMb());
        assertEquals(BASE_FLUID, entry.inputAmountMb());
        assertEquals(TARGET_OUTPUT, entry.outputAmountMb());
    }

    @Test
    void fragmentBudgetIsTheRecipesOwnCostDoubled() {
        List<StructureReactorJeiRecipe> entries =
                StructureReactorJeiRecipes.build(List.of(recipe("tier_three", SHALLOW, null, 3)));

        assertEquals(3, entries.get(0).fragmentCount());
        assertEquals(6, entries.get(0).maxExtraFragments());
    }

    @Test
    void theLongestSequenceSizesTheReservedRows() {
        assertEquals(0, StructureReactorJeiRecipes.longestSequence(List.of()));
        assertEquals(
                ALTERNATE.size(),
                StructureReactorJeiRecipes.longestSequence(
                        List.of(recipe("shallow", SHALLOW, null), recipe("tier_five", DEEP, ALTERNATE))));
    }

    @Test
    void rowCountFallsBackForAnEmptyRegistryAndIsCapped() {
        assertEquals(StructureReactorJeiRecipes.FALLBACK_ROWS, StructureReactorJeiRecipes.visibleRows(0));
        assertEquals(1, StructureReactorJeiRecipes.visibleRows(1));
        assertEquals(6, StructureReactorJeiRecipes.visibleRows(6));
        assertEquals(
                StructureReactorJeiRecipes.MAX_ROWS,
                StructureReactorJeiRecipes.visibleRows(StructureReactorJeiRecipes.MAX_ROWS + 8));
    }

    private static List<StateStep<String>> sequence(StateId... states) {
        List<StateStep<String>> steps = new ArrayList<>();
        for (StateId state : states) {
            steps.add(new StateStep<>(state, OperationMatcher.of(value -> value.equals(state.name()))));
        }
        return steps;
    }

    private static StructureReactorRecipe<String> recipe(
            String path, List<StateStep<String>> primary, List<StateStep<String>> alternate) {
        return recipe(path, primary, alternate, 1);
    }

    private static StructureReactorRecipe<String> recipe(
            String path,
            List<StateStep<String>> primary,
            List<StateStep<String>> alternate,
            int fragmentCount) {
        return new StructureReactorRecipe<>(
                ResourceLocation.fromNamespaceAndPath("dimension_tech", path),
                null,
                null,
                null,
                fragmentCount,
                BASE_FLUID,
                TARGET_OUTPUT,
                primary,
                alternate);
    }
}
