package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.suntide_20210418.dimensiontech.loot.expectation.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class DistributionalLootPool1201Test {
    private static final LootAnalysisContext CONTEXT =
            new LootAnalysisContext(null, null, null, 0.0F, Map.of(), null, null, null, Map.of());

    @Test
    void zeroRollsProveUnknownEntryUnreachable() {
        JsonObject pool =
                JsonParser.parseString("{\"rolls\":0,\"entries\":[{\"type\":\"example:unknown\"}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.execute(
                        pool, CONTEXT, 100, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals(
                Map.of(List.of(), ExactProbability.ONE), result.distribution().marginal().masses());
    }

    @Test
    void anyPositiveRollMassMakesUnknownEntryUnsupported() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":{\"type\":\"minecraft:uniform\",\"min\":0,\"max\":1},"
                                        + "\"entries\":[{\"type\":\"example:unknown\"}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.execute(
                        pool, CONTEXT, 100, ignored -> List.of(), "/pools/0");

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
                DistributionalLootPool1201.execute(
                        pool, CONTEXT, 100, ignored -> List.of(), "/pools/0");

        assertFalse(result.supported());
        assertEquals("/pools/0/entries/0", result.pointer());
    }

    @Test
    void zeroLuckBonusCallPrecedesWeightedSelection() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,"
                                    + "\"bonus_rolls\":{\"type\":\"minecraft:uniform\",\"min\":0.0,\"max\":1.0},"
                                    + "\"entries\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\"}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.execute(
                        pool, CONTEXT, 100, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertTrue(
                result.distribution().masses().keySet().stream()
                        .allMatch(
                                outcome ->
                                        outcome.calls().stream()
                                                .map(ExactRandomSemantics1201.RandomCall::method)
                                                .toList()
                                                .equals(
                                                        List.of(
                                                                ExactRandomSemantics1201
                                                                        .RandomMethod.NEXT_FLOAT,
                                                                ExactRandomSemantics1201
                                                                        .RandomMethod
                                                                        .NEXT_INT_BOUND))));
        assertEquals(
                ExactProbability.of(1, 2),
                result.distribution().marginal().masses().values().iterator().next());
    }

    @Test
    void luckAndQualityAdjustCandidateWeightsBeforeBoundedSelection() {
        LootAnalysisContext context =
                new LootAnalysisContext(
                        null, null, null, 2.0F, Map.of(), null, null, null, Map.of());
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\",\"weight\":1,\"quality\":2},"
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\",\"weight\":3,\"quality\":-1}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.execute(
                        pool, context, 100, ignored -> List.of(), "/pools/0");
        Map<String, ExactProbability> masses = new java.util.LinkedHashMap<>();
        result.distribution()
                .masses()
                .forEach(
                        (outcome, mass) ->
                                masses.merge(
                                        outcome.value().get(0).entry().get("name").getAsString(),
                                        mass,
                                        ExactProbability::add));

        assertTrue(result.supported());
        assertEquals(ExactProbability.of(5, 6), masses.get("minecraft:stone"));
        assertEquals(ExactProbability.of(1, 6), masses.get("minecraft:dirt"));
        assertTrue(
                result.distribution().masses().keySet().stream()
                        .allMatch(
                                outcome ->
                                        outcome.calls()
                                                .equals(
                                                        List.of(
                                                                new ExactRandomSemantics1201
                                                                        .RandomCall(
                                                                        ExactRandomSemantics1201
                                                                                .RandomMethod
                                                                                .NEXT_INT_BOUND,
                                                                        6)))));
    }

    @Test
    void randomPoolConditionRollPmfAndBonusFloorComposeExactly() {
        LootAnalysisContext context =
                new LootAnalysisContext(
                        null, null, null, 2.0F, Map.of(), null, null, null, Map.of());
        JsonObject pool =
                JsonParser.parseString(
                                "{\"conditions\":[{\"condition\":\"minecraft:random_chance\",\"chance\":0.5}],"
                                    + "\"rolls\":{\"type\":\"minecraft:uniform\",\"min\":1,\"max\":2},"
                                    + "\"bonus_rolls\":0.75,"
                                    + "\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.execute(
                        pool, context, 100, ignored -> List.of(), "/pools/0");
        Map<Integer, ExactProbability> lengths = new java.util.LinkedHashMap<>();
        result.distribution()
                .masses()
                .forEach(
                        (outcome, mass) ->
                                lengths.merge(outcome.value().size(), mass, ExactProbability::add));

        assertTrue(result.supported());
        assertEquals(
                Map.of(
                        0, ExactProbability.of(1, 2),
                        2, ExactProbability.of(1, 4),
                        3, ExactProbability.of(1, 4)),
                lengths);
        assertTrue(
                result.distribution().masses().keySet().stream()
                        .allMatch(
                                outcome ->
                                        outcome.calls().size()
                                                == (outcome.value().isEmpty() ? 1 : 2)));
    }

    @Test
    void alternativesSkipUnknownAfterCertainExpansion() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{"
                                    + "\"type\":\"minecraft:alternatives\",\"children\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                                    + "{\"type\":\"example:unknown\"}]}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.execute(
                        pool, CONTEXT, 100, ignored -> List.of(), "/pools/0");

        assertTrue(result.supported());
        assertEquals(
                "minecraft:stone",
                result.distribution()
                        .marginal()
                        .masses()
                        .keySet()
                        .iterator()
                        .next()
                        .get(0)
                        .entry()
                        .get("name")
                        .getAsString());
    }

    @Test
    void groupExpandsAllChildrenIntoOneWeightedCandidateList() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:group\",\"children\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\",\"weight\":1},"
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\",\"weight\":3}]}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.execute(
                        pool, CONTEXT, 100, ignored -> List.of(), "/pools/0");
        Map<String, ExactProbability> masses = new java.util.LinkedHashMap<>();
        result.distribution()
                .masses()
                .forEach(
                        (outcome, mass) ->
                                masses.merge(
                                        outcome.value().get(0).entry().get("name").getAsString(),
                                        mass,
                                        ExactProbability::add));

        assertTrue(result.supported());
        assertEquals(ExactProbability.of(1, 4), masses.get("minecraft:stone"));
        assertEquals(ExactProbability.of(3, 4), masses.get("minecraft:dirt"));
    }

    @Test
    void selectedEntryCallsOccurBeforeTheNextRollSelection() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":2,\"entries\":["
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:stone\"},"
                                    + "{\"type\":\"minecraft:item\",\"name\":\"minecraft:dirt\"}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.execute(
                        pool,
                        CONTEXT,
                        1_000,
                        ignored -> List.of(),
                        null,
                        (selected, maxStates) ->
                                DistributionalLootPool1201.SelectionEvaluation.exact(
                                        RandomTraceDistribution.fromRandomResult(
                                                        ExactRandomSemantics1201.nextBoolean())
                                                .flatMap(
                                                        value ->
                                                                RandomTraceDistribution.singleton(
                                                                        List.of(
                                                                                selected.entry()
                                                                                                .get(
                                                                                                        "name")
                                                                                                .getAsString()
                                                                                        + ":"
                                                                                        + value)),
                                                        maxStates)),
                        "/pools/0");

        assertTrue(result.supported());
        assertTrue(
                result.distribution().masses().keySet().stream()
                        .allMatch(
                                outcome ->
                                        outcome.calls().stream()
                                                .map(ExactRandomSemantics1201.RandomCall::method)
                                                .toList()
                                                .equals(
                                                        List.of(
                                                                ExactRandomSemantics1201
                                                                        .RandomMethod
                                                                        .NEXT_INT_BOUND,
                                                                ExactRandomSemantics1201
                                                                        .RandomMethod.NEXT_BOOLEAN,
                                                                ExactRandomSemantics1201
                                                                        .RandomMethod
                                                                        .NEXT_INT_BOUND,
                                                                ExactRandomSemantics1201
                                                                        .RandomMethod
                                                                        .NEXT_BOOLEAN))));
    }

    @Test
    void firstMomentPathDoesNotMaterializeRepeatedRollProducts() {
        StringBuilder entries = new StringBuilder();
        for (int index = 0; index < 11; index++) {
            if (index > 0) entries.append(',');
            entries.append("{\"type\":\"minecraft:item\",\"name\":\"test:item_")
                    .append(index)
                    .append("\"}");
        }
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":{\"type\":\"minecraft:uniform\",\"min\":2,\"max\":4},"
                                        + "\"entries\":["
                                        + entries
                                        + "]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.expectation(
                        pool,
                        CONTEXT,
                        20,
                        ignored -> List.of(),
                        null,
                        (selected, ignored) ->
                                DistributionalLootPool1201.ExpectedSelectionEvaluation.exact(
                                        Map.of(
                                                selected.entry().get("name").getAsString(),
                                                ExactProbability.ONE),
                                        false),
                        "/pools/0");

        assertTrue(result.supported());
        assertTrue(result.hasRandomCalls());
        assertEquals(11, result.occurrences().size());
        assertTrue(
                result.occurrences().values().stream()
                        .allMatch(mass -> mass.equals(ExactProbability.of(3, 11))));
    }

    @Test
    void firstMomentPathPropagatesRepeatedMapAllocationUpperBound() {
        JsonObject pool =
                JsonParser.parseString(
                                "{\"rolls\":2,\"entries\":[{\"type\":\"minecraft:item\","
                                        + "\"name\":\"minecraft:map\"}]}")
                        .getAsJsonObject();

        var result =
                DistributionalLootPool1201.expectation(
                        pool,
                        CONTEXT,
                        20,
                        ignored -> List.of(),
                        null,
                        (selected, ignored) ->
                                DistributionalLootPool1201.ExpectedSelectionEvaluation.exact(
                                        Map.of("filled_map", ExactProbability.ONE), false, 1, 1),
                        "/pools/0");

        assertTrue(result.supported());
        assertEquals(2, result.maxOutputs());
        assertEquals(2, result.maxMapAllocations());
    }
}
