package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class StatefulLootPool1201Test {
    private static final LootAnalysisContext CONTEXT =
            new LootAnalysisContext(null, null, null, 0.0F, Map.of(), null, null, null, Map.of());

    @Test
    void repeatsExpansionAndCarriesSelectionStateIntoTheNextRoll() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":2,\"entries\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\"}]}"
                                    + " ")
                        .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(17L, 29L);

        var result =
                StatefulLootPool1201.execute(
                        pool, CONTEXT, initial, ignored -> List.of(), "/pools/0");
        var first = initial.nextInt(2);
        var second = first.state().nextInt(2);

        assertTrue(result.supported());
        assertEquals(
                first.value() == 0 ? "minecraft:stone" : "minecraft:dirt",
                result.selectedEntries().get(0).get("name").getAsString());
        assertEquals(
                second.value() == 0 ? "minecraft:stone" : "minecraft:dirt",
                result.selectedEntries().get(1).get("name").getAsString());
        assertEquals(second.state(), result.randomState());
    }

    @Test
    void singlePositiveCandidateDoesNotConsumeSelectionRandomness() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":["
                                    + "{\"type\":\"minecraft:empty\",\"weight\":0},{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"}]}"
                                    + " ")
                        .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(31L, 47L);

        var result =
                StatefulLootPool1201.execute(
                        pool, CONTEXT, initial, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals(initial, result.randomState());
        assertEquals(1, result.selectedEntries().size());
    }

    @Test
    void luckAndQualityUseAdjustedTotalForConcreteBoundedSelection() {
        LootAnalysisContext context =
                new LootAnalysisContext(
                        null, null, null, 2.0F, Map.of(), null, null, null, Map.of());
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\",\"weight\":1,\"quality\":2},"
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\",\"weight\":3,\"quality\":-1}]}")
                        .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(53L, 79L);
        var draw = initial.nextInt(6);

        var result =
                StatefulLootPool1201.execute(
                        pool, context, initial, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals(
                draw.value() < 5 ? "minecraft:stone" : "minecraft:dirt",
                result.selectedEntries().get(0).get("name").getAsString());
        assertEquals(draw.state(), result.randomState());
    }

    @Test
    void deterministicallyUnreachableUnknownEntryIsSkipped() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{\"type\":\"example:unknown\","
                                    + "\"conditions\":[{\"condition\":\"minecraft:always_false\"}]}]}")
                        .getAsJsonObject();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        new XoroshiroState1201(1L, 2L),
                        ignored -> List.of(),
                        "/pools/0");

        assertTrue(result.supported());
        assertTrue(result.selectedEntries().isEmpty());
    }

    @Test
    void reachableUnknownEntryFailsAtItsExactPointer() {
        JsonObject pool =
                JsonParser.parseString(
                                "{" + "\"rolls\":1,\"entries\":[{\"type\":\"example:unknown\"}]}")
                        .getAsJsonObject();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        new XoroshiroState1201(1L, 2L),
                        ignored -> List.of(),
                        "/pools/0");

        assertFalse(result.supported());
        assertEquals("/pools/0/entries/0", result.pointer());
    }

    @Test
    void vanillaEntryPathInAnotherNamespaceRemainsUnknown() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{\"type\":\"example:item\","
                                        + "\"name\":\"minecraft:stone\"}]}")
                        .getAsJsonObject();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        new XoroshiroState1201(13L, 21L),
                        ignored -> List.of(),
                        "/pools/0");

        assertFalse(result.supported());
        assertEquals("/pools/0/entries/0", result.pointer());
    }

    @Test
    void alternativesShortCircuitPreventsUnknownChildFromBeingParsed() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{"
                                    + "\"type\":\"minecraft:alternatives\",\"children\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                                    + "{\"type\":\"example:unknown\"}]}]}")
                        .getAsJsonObject();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        new XoroshiroState1201(3L, 5L),
                        ignored -> List.of(),
                        "/pools/0");

        assertTrue(result.supported());
        assertEquals("minecraft:stone", result.selectedEntries().get(0).get("name").getAsString());
    }

    @Test
    void evaluatesConditionThenRollsThenBonusUsingOneContinuation() {
        LootAnalysisContext luckContext =
                new LootAnalysisContext(
                        null, null, null, 2.0F, Map.of(), null, null, null, Map.of());
        JsonObject pool =
                JsonParser.parseString(
                                "{\"conditions\":[{\"condition\":\"minecraft:random_chance\",\"chance\":1.0}],"
                                    + "\"rolls\":{\"type\":\"minecraft:uniform\",\"min\":1,\"max\":2},"
                                    + "\"bonus_rolls\":{\"type\":\"minecraft:uniform\",\"min\":0.5,\"max\":1.0},"
                                    + "\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"}]}")
                        .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(8L, 13L);
        var conditionDraw = initial.nextFloat();
        var rollsDraw = conditionDraw.state().nextIntInclusive(1, 2);
        var bonusDraw = rollsDraw.state().nextFloat();
        float bonus = bonusDraw.value() * 0.5F + 0.5F;
        int expectedRolls = rollsDraw.value() + net.minecraft.util.Mth.floor(bonus * 2.0F);

        var result =
                StatefulLootPool1201.execute(
                        pool, luckContext, initial, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals(expectedRolls, result.selectedEntries().size());
        assertEquals(bonusDraw.state(), result.randomState());
    }

    @Test
    void zeroLuckStillConsumesUniformBonusBeforeCandidateSelection() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,"
                                    + "\"bonus_rolls\":{\"type\":\"minecraft:uniform\",\"min\":0.0,\"max\":1.0},"
                                    + "\"entries\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\"}]}")
                        .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(13L, 21L);
        var bonusDraw = initial.nextFloat();
        var selectionDraw = bonusDraw.state().nextInt(2);

        var result =
                StatefulLootPool1201.execute(
                        pool, CONTEXT, initial, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals(
                selectionDraw.value() == 0 ? "minecraft:stone" : "minecraft:dirt",
                result.selectedEntries().get(0).get("name").getAsString());
        assertEquals(selectionDraw.state(), result.randomState());
    }

    @Test
    void expandedTagAddsEachItemAsAWeightedCandidateAfterOneConditionCall() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{"
                                    + "\"type\":\"minecraft:tag\",\"name\":\"example:ores\",\"expand\":true,"
                                    + "\"conditions\":[{\"condition\":\"minecraft:random_chance\",\"chance\":1.0}]}]}")
                        .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(21L, 34L);
        JsonObject iron =
                JsonParser.parseString(
                                "{\"type\":\"minecraft:item\",\"name\":\"minecraft:iron_ore\"}")
                        .getAsJsonObject();
        JsonObject gold =
                JsonParser.parseString(
                                "{\"type\":\"minecraft:item\",\"name\":\"minecraft:gold_ore\"}")
                        .getAsJsonObject();
        var conditionDraw = initial.nextFloat();
        var selectionDraw = conditionDraw.state().nextInt(2);

        var result =
                StatefulLootPool1201.execute(
                        pool, CONTEXT, initial, ignored -> List.of(iron, gold), "/pools/0");

        assertTrue(result.supported());
        assertEquals(
                selectionDraw.value() == 0 ? "minecraft:iron_ore" : "minecraft:gold_ore",
                result.selectedEntries().get(0).get("name").getAsString());
        assertEquals(selectionDraw.state(), result.randomState());
    }

    @Test
    void selectedEntryRandomCallsAdvanceTheStateBeforeTheNextRoll() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":2,\"entries\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\"}]}")
                        .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(55L, 89L);
        var firstSelection = initial.nextInt(2);
        var firstFunction = firstSelection.state().nextBoolean();
        var secondSelection = firstFunction.state().nextInt(2);
        var secondFunction = secondSelection.state().nextBoolean();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        initial,
                        ignored -> List.of(),
                        (entry, pointer, state) -> {
                            var draw = state.nextBoolean();
                            return StatefulLootPool1201.SelectionResult.exact(
                                    List.of(entry.get("name").getAsString() + ":" + draw.value()),
                                    draw.state());
                        },
                        "/pools/0");

        assertTrue(result.supported());
        assertEquals(
                List.of(
                        (firstSelection.value() == 0 ? "minecraft:stone" : "minecraft:dirt")
                                + ":"
                                + firstFunction.value(),
                        (secondSelection.value() == 0 ? "minecraft:stone" : "minecraft:dirt")
                                + ":"
                                + secondFunction.value()),
                result.outputs());
        assertEquals(secondFunction.state(), result.randomState());
    }

    @Test
    void sequenceFailureDoesNotRetractCandidatesAlreadySentToTheConsumer() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{"
                                    + "\"type\":\"minecraft:sequence\",\"children\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\","
                                    + "\"conditions\":[{\"condition\":\"minecraft:always_false\"}]}]}]}")
                        .getAsJsonObject();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        new XoroshiroState1201(144L, 233L),
                        ignored -> List.of(),
                        "/pools/0");

        assertTrue(result.supported());
        assertEquals(1, result.selectedEntries().size());
        assertEquals("minecraft:stone", result.selectedEntries().get(0).get("name").getAsString());
    }

    @Test
    void positiveRollRejectsNonArrayEntriesAtTheFieldPointer() {
        JsonObject pool =
                JsonParser.parseString("{\"rolls\":1,\"entries\":{}}")
                        .getAsJsonObject();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        new XoroshiroState1201(1L, 2L),
                        ignored -> List.of(),
                        "/pools/0");

        assertFalse(result.supported());
        assertEquals("/pools/0/entries", result.pointer());
    }

    @Test
    void zeroRollProvesMalformedEntriesUnreachable() {
        JsonObject pool =
                JsonParser.parseString("{\"rolls\":0,\"entries\":{}}")
                        .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(3L, 5L);

        var result =
                StatefulLootPool1201.execute(
                        pool, CONTEXT, initial, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertTrue(result.selectedEntries().isEmpty());
        assertEquals(initial, result.randomState());
    }

    @Test
    void malformedEntryTypeAndNameReturnExactPointers() {
        JsonObject malformedType =
                JsonParser.parseString("{\"rolls\":1,\"entries\":[{\"type\":{}}]}")
                        .getAsJsonObject();
        JsonObject malformedName =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":{}}]}")
                        .getAsJsonObject();

        var typeResult =
                StatefulLootPool1201.execute(
                        malformedType,
                        CONTEXT,
                        new XoroshiroState1201(8L, 13L),
                        ignored -> List.of(),
                        "/pools/0");
        var nameResult =
                StatefulLootPool1201.execute(
                        malformedName,
                        CONTEXT,
                        new XoroshiroState1201(8L, 13L),
                        ignored -> List.of(),
                        "/pools/0");

        assertFalse(typeResult.supported());
        assertEquals("/pools/0/entries/0", typeResult.pointer());
        assertFalse(nameResult.supported());
        assertEquals("/pools/0/entries/0/name", nameResult.pointer());
    }

    @Test
    void zeroWeightSkipsMalformedTypeSpecificFields() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":["
                                        + "{\"type\":\"example:unknown\",\"weight\":0},"
                                        + "{\"type\":\"minecraft:item\",\"name\":{},\"weight\":0},"
                                        + "{\"type\":{},\"weight\":0}]}")
                        .getAsJsonObject();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        new XoroshiroState1201(8L, 13L),
                        ignored -> List.of(),
                        "/pools/0");

        assertTrue(result.supported());
        assertTrue(result.selectedEntries().isEmpty());
    }

    @Test
    void malformedWeightQualityAndExpandReturnFieldPointers() {
        JsonObject malformedWeight =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\",\"weight\":{}}]}")
                        .getAsJsonObject();
        JsonObject malformedQuality =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\",\"quality\":{}}]}")
                        .getAsJsonObject();
        JsonObject malformedExpand =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:tag\",\"name\":\"example:ores\",\"expand\":{}}]}")
                        .getAsJsonObject();

        var weightResult =
                StatefulLootPool1201.execute(
                        malformedWeight,
                        CONTEXT,
                        new XoroshiroState1201(21L, 34L),
                        ignored -> List.of(),
                        "/pools/0");
        var qualityResult =
                StatefulLootPool1201.execute(
                        malformedQuality,
                        CONTEXT,
                        new XoroshiroState1201(21L, 34L),
                        ignored -> List.of(),
                        "/pools/0");
        var expandResult =
                StatefulLootPool1201.execute(
                        malformedExpand,
                        CONTEXT,
                        new XoroshiroState1201(21L, 34L),
                        ignored -> List.of(),
                        "/pools/0");

        assertFalse(weightResult.supported());
        assertEquals("/pools/0/entries/0/weight", weightResult.pointer());
        assertFalse(qualityResult.supported());
        assertEquals("/pools/0/entries/0/quality", qualityResult.pointer());
        assertFalse(expandResult.supported());
        assertEquals("/pools/0/entries/0/expand", expandResult.pointer());
    }

    @Test
    void compositeChildrenMustBeAnArrayWhenReached() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:group\",\"children\":{}}]}")
                        .getAsJsonObject();

        var result =
                StatefulLootPool1201.execute(
                        pool,
                        CONTEXT,
                        new XoroshiroState1201(55L, 89L),
                        ignored -> List.of(),
                        "/pools/0");

        assertFalse(result.supported());
        assertEquals("/pools/0/entries/0/children", result.pointer());
    }
}
