package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatefulLootPool1201Test {
    private static final LootAnalysisContext CONTEXT =
            new LootAnalysisContext(null, null, null, 0.0F, Map.of(), null, null, null);

    @Test
    void repeatsExpansionAndCarriesSelectionStateIntoTheNextRoll() {
        JsonObject pool = JsonParser.parseString("{"
                        + "\"rolls\":2,\"entries\":["
                        + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                        + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\"}]} ")
                .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(17L, 29L);

        var result = StatefulLootPool1201.execute(pool, CONTEXT, initial, ignored -> List.of(), "/pools/0");
        var first = initial.nextInt(2);
        var second = first.state().nextInt(2);

        assertTrue(result.supported());
        assertEquals(first.value() == 0 ? "minecraft:stone" : "minecraft:dirt",
                result.selectedEntries().get(0).get("name").getAsString());
        assertEquals(second.value() == 0 ? "minecraft:stone" : "minecraft:dirt",
                result.selectedEntries().get(1).get("name").getAsString());
        assertEquals(second.state(), result.randomState());
    }

    @Test
    void singlePositiveCandidateDoesNotConsumeSelectionRandomness() {
        JsonObject pool = JsonParser.parseString("{"
                        + "\"rolls\":1,\"entries\":["
                        + "{\"type\":\"minecraft:empty\",\"weight\":0},"
                        + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"}]} ")
                .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(31L, 47L);

        var result = StatefulLootPool1201.execute(pool, CONTEXT, initial, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals(initial, result.randomState());
        assertEquals(1, result.selectedEntries().size());
    }

    @Test
    void deterministicallyUnreachableUnknownEntryIsSkipped() {
        JsonObject pool = JsonParser.parseString("{"
                        + "\"rolls\":1,\"entries\":[{"
                        + "\"type\":\"example:unknown\","
                        + "\"conditions\":[{\"condition\":\"minecraft:always_false\"}]}]}")
                .getAsJsonObject();

        var result = StatefulLootPool1201.execute(
                pool, CONTEXT, new XoroshiroState1201(1L, 2L), ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertTrue(result.selectedEntries().isEmpty());
    }

    @Test
    void reachableUnknownEntryFailsAtItsExactPointer() {
        JsonObject pool = JsonParser.parseString("{"
                        + "\"rolls\":1,\"entries\":[{\"type\":\"example:unknown\"}]}")
                .getAsJsonObject();

        var result = StatefulLootPool1201.execute(
                pool, CONTEXT, new XoroshiroState1201(1L, 2L), ignored -> List.of(), "/pools/0");

        assertFalse(result.supported());
        assertEquals("/pools/0/entries/0", result.pointer());
    }

    @Test
    void alternativesShortCircuitPreventsUnknownChildFromBeingParsed() {
        JsonObject pool = JsonParser.parseString("{\"rolls\":1,\"entries\":[{"
                        + "\"type\":\"minecraft:alternatives\",\"children\":["
                        + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                        + "{\"type\":\"example:unknown\"}]}]}")
                .getAsJsonObject();

        var result = StatefulLootPool1201.execute(
                pool, CONTEXT, new XoroshiroState1201(3L, 5L), ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals("minecraft:stone", result.selectedEntries().get(0).get("name").getAsString());
    }

    @Test
    void evaluatesConditionThenRollsThenBonusUsingOneContinuation() {
        LootAnalysisContext luckContext =
                new LootAnalysisContext(null, null, null, 2.0F, Map.of(), null, null, null);
        JsonObject pool = JsonParser.parseString("{"
                        + "\"conditions\":[{\"condition\":\"minecraft:random_chance\",\"chance\":1.0}],"
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

        var result = StatefulLootPool1201.execute(
                pool, luckContext, initial, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals(expectedRolls, result.selectedEntries().size());
        assertEquals(bonusDraw.state(), result.randomState());
    }

    @Test
    void expandedTagAddsEachItemAsAWeightedCandidateAfterOneConditionCall() {
        JsonObject pool = JsonParser.parseString("{\"rolls\":1,\"entries\":[{"
                        + "\"type\":\"minecraft:tag\",\"name\":\"example:ores\",\"expand\":true,"
                        + "\"conditions\":[{\"condition\":\"minecraft:random_chance\",\"chance\":1.0}]}]}")
                .getAsJsonObject();
        XoroshiroState1201 initial = new XoroshiroState1201(21L, 34L);
        JsonObject iron = JsonParser.parseString(
                        "{\"type\":\"minecraft:item\",\"name\":\"minecraft:iron_ore\"}")
                .getAsJsonObject();
        JsonObject gold = JsonParser.parseString(
                        "{\"type\":\"minecraft:item\",\"name\":\"minecraft:gold_ore\"}")
                .getAsJsonObject();
        var conditionDraw = initial.nextFloat();
        var selectionDraw = conditionDraw.state().nextInt(2);

        var result = StatefulLootPool1201.execute(
                pool, CONTEXT, initial, ignored -> List.of(iron, gold), "/pools/0");

        assertTrue(result.supported());
        assertEquals(selectionDraw.value() == 0 ? "minecraft:iron_ore" : "minecraft:gold_ore",
                result.selectedEntries().get(0).get("name").getAsString());
        assertEquals(selectionDraw.state(), result.randomState());
    }
}
