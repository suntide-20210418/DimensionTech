package com.suntide_20210418.dimensiontech.gametest;

import com.google.gson.JsonParser;
import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.loot.expectation.DistributionalCondition1201;
import com.suntide_20210418.dimensiontech.loot.expectation.DistributionalFunction1201;
import com.suntide_20210418.dimensiontech.loot.expectation.DistributionalLootTableExecutor1201;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactEnchantmentSemantics1201;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactRandomSemantics1201.RandomCall;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactRandomSemantics1201.RandomMethod;
import com.suntide_20210418.dimensiontech.loot.expectation.LootAnalysisContext;
import com.suntide_20210418.dimensiontech.loot.expectation.LootExpectationResult;
import com.suntide_20210418.dimensiontech.loot.expectation.PersistentRandomSequenceSnapshot1201;
import com.suntide_20210418.dimensiontech.loot.expectation.RuntimeLootAstSource;
import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.loot.expectation.StackState;
import com.suntide_20210418.dimensiontech.loot.expectation.StatefulCondition1201;
import com.suntide_20210418.dimensiontech.loot.expectation.StatefulFunction1201;
import com.suntide_20210418.dimensiontech.loot.expectation.StatefulLootTableExecutor1201;
import com.suntide_20210418.dimensiontech.loot.expectation.TerminalStackKey;
import com.suntide_20210418.dimensiontech.loot.expectation.TerminalStackMeasure;
import com.suntide_20210418.dimensiontech.loot.expectation.XoroshiroState1201;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LootExpectationGameTests {
    private LootExpectationGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void zeroCountStackStateRetainsItemForLaterFunctions(GameTestHelper helper) {
        StackState state = new StackState(new ItemStack(Items.STONE, 3));
        StackState restoredState = state.withCount(0).withCount(3);
        ItemStack restored = restoredState.stack();
        if (restored.getItem() != Items.STONE || restored.getCount() != 3) {
            helper.fail("Zero-count stack state lost its item identity: " + restored);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void stackStateKeepsCountsOutsideVanillaNbtByteRange(GameTestHelper helper) {
        StackState lower = new StackState(new ItemStack(Items.STONE, 72));
        StackState higher = new StackState(new ItemStack(Items.STONE, 200));
        if (lower.count() != 72 || higher.count() != 200 || lower.equals(higher)) {
            helper.fail("StackState did not retain exact outer counts: " + lower + ", " + higher);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void identicalCountThreeStacksHaveTwoOccurrencesAndSixItems(
            GameTestHelper helper) {
        StackMeasure measure = new StackMeasure();
        measure.add(new StackState(new ItemStack(Items.STONE, 3)), ExactProbability.ONE);
        measure.add(new StackState(new ItemStack(Items.STONE, 3)), ExactProbability.ONE);

        if (measure.values().size() != 1) {
            helper.fail("Expected one distinct stack state, got " + measure.values().size());
        }
        var entry = measure.values().entrySet().iterator().next();
        if (entry.getKey().count() != 3) {
            helper.fail("Expected stack count 3, got " + entry.getKey().count());
        }
        if (!entry.getValue().equals(ExactProbability.of(2, 1))) {
            helper.fail("Expected two stack occurrences, got " + entry.getValue());
        }
        double itemCount = entry.getValue().doubleValue() * entry.getKey().count();
        if (itemCount != 6.0D) {
            helper.fail("Expected item count 6, got " + itemCount);
        }

        TerminalStackMeasure terminal = TerminalStackMeasure.from(measure);
        if (terminal.values().size() != 1) {
            helper.fail("Expected one terminal state, got " + terminal.values().size());
        }
        TerminalStackKey terminalKey = terminal.values().keySet().iterator().next();
        if (terminalKey.item() != Items.STONE
                || terminalKey.count() != 3
                || terminal.values().get(terminalKey) == null
                || !terminal.values().get(terminalKey).equals(ExactProbability.of(2, 1))) {
            helper.fail("Terminal projection lost count=3 occurrence mass: " + terminal.values());
        }
        if (!terminal.exactItemCount(Items.STONE).equals(ExactProbability.of(6, 1))) {
            helper.fail("Terminal item count was not exactly 6: " + terminal.values());
        }
        if (!terminal.exactRarityWeightedValue(rarity -> 10L).equals(ExactProbability.of(60, 1))) {
            helper.fail("Terminal rarity-weighted value was not exactly 60: " + terminal.values());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void mixedFullAndTerminalMeasuresRetainOccurrenceMass(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.STONE, 3);
        StackMeasure full = new StackMeasure();
        full.add(new StackState(stack), ExactProbability.ONE);

        TerminalStackKey key = TerminalStackKey.from(stack);
        TerminalStackMeasure mixed =
                TerminalStackMeasure.from(full)
                        .plus(TerminalStackMeasure.of(Map.of(key, ExactProbability.ONE)));

        ExactProbability expectedOccurrences = ExactProbability.of(2, 1);
        if (!expectedOccurrences.equals(mixed.values().get(key))) {
            helper.fail("Mixed full/terminal output lost mass: " + mixed.values());
        }
        if (!ExactProbability.of(6, 1).equals(mixed.exactItemCount(Items.STONE))) {
            helper.fail("Mixed full/terminal output changed item count: " + mixed.values());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void executorRetainsMixedFullAndTerminalOutputs(GameTestHelper helper) {
        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/mixed_terminal_output");
        var result =
                DistributionalLootTableExecutor1201.evaluateMarginalCalls(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        10_000);

        if (!result.supported()) {
            helper.fail("Mixed terminal table was unsupported: " + result.diagnostics());
        }
        if (result.fullStackMeasureAvailable()) {
            helper.fail("Terminally enchanted output was incorrectly exposed as a full measure");
        }
        if (!ExactProbability.of(3, 1)
                .equals(result.terminalMeasure().exactItemCount(Items.STONE))) {
            helper.fail("Full StackState output was lost: " + result.terminalMeasure().values());
        }
        if (!ExactProbability.ONE.equals(
                result.terminalMeasure().exactItemCount(Items.DIAMOND_SWORD))) {
            helper.fail(
                    "Terminally aggregated output was lost: " + result.terminalMeasure().values());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void nestedTerminalTablePropagatesWithoutMaterializingEnchantments(
            GameTestHelper helper) {
        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/nested_terminal_root");
        LootExpectationResult result =
                DistributionalLootTableExecutor1201.evaluate(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        1_000);
        if (result.status() != AnalysisStatus.EXACT) {
            helper.fail("Nested terminal table was unsupported: " + result.diagnostics());
        }
        if (result.fullStackMeasureAvailable()) {
            helper.fail("Nested terminal enchantment was unexpectedly materialized");
        }
        if (!result.terminalMeasure()
                .exactItemCount(Items.DIAMOND_SWORD)
                .equals(ExactProbability.ONE)) {
            helper.fail(
                    "Expected exactly one nested diamond sword, got "
                            + result.terminalMeasure().values());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void multiplePoolsProduceDistinctStackStatesAndExactItemCounts(
            GameTestHelper helper) {
        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/multi_stack");
        LootAnalysisContext context =
                LootAnalysisContext.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F);

        LootExpectationResult result =
                DistributionalLootTableExecutor1201.evaluate(
                        helper.getLevel().getServer(), tableId, context, 1_000);

        if (result.status() != AnalysisStatus.EXACT) {
            helper.fail("Multi-stack table was unsupported: " + result.diagnostics());
        }
        double stoneCount =
                result.measure().values().entrySet().stream()
                        .filter(entry -> entry.getKey().stack().is(Items.STONE))
                        .mapToDouble(
                                entry -> entry.getValue().doubleValue() * entry.getKey().count())
                        .sum();
        double dirtCount =
                result.measure().values().entrySet().stream()
                        .filter(entry -> entry.getKey().stack().is(Items.DIRT))
                        .mapToDouble(
                                entry -> entry.getValue().doubleValue() * entry.getKey().count())
                        .sum();
        if (result.measure().values().size() != 2 || stoneCount != 2.0D || dirtCount != 3.0D) {
            helper.fail(
                    "Expected stone=2 and dirt=3 in two states, got " + result.measure().values());
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void distributionalFunctionsPreserveCountAndLazyReachability(
            GameTestHelper helper) {
        LootAnalysisContext context =
                LootAnalysisContext.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F);
        StackState stone = new StackState(new ItemStack(Items.STONE));
        var setCount =
                DistributionalFunction1201.applyAll(
                        stone,
                        JsonParser.parseString(
                                "[{\"function\":\"minecraft:set_count\","
                                    + "\"count\":{\"type\":\"minecraft:uniform\",\"min\":2,\"max\":4}}]"),
                        context,
                        100,
                        "/functions");
        if (!setCount.supported()) {
            helper.fail("set_count was unsupported: " + setCount.message());
        }
        java.util.Map<Integer, ExactProbability> counts = new java.util.LinkedHashMap<>();
        setCount.distribution()
                .marginal()
                .masses()
                .forEach((state, mass) -> counts.put(state.count(), mass));
        java.util.Map<Integer, ExactProbability> expected =
                java.util.Map.of(
                        2, ExactProbability.of(1, 3),
                        3, ExactProbability.of(1, 3),
                        4, ExactProbability.of(1, 3));
        if (!counts.equals(expected)) {
            helper.fail("Unexpected set_count distribution: " + counts);
        }

        var skipped =
                DistributionalFunction1201.applyAll(
                        stone,
                        JsonParser.parseString(
                                "[{\"function\":\"example:unknown\","
                                    + "\"conditions\":[{\"condition\":\"minecraft:always_false\"}]}]"),
                        context,
                        100,
                        "/functions");
        if (!skipped.supported()) {
            helper.fail("Unreachable function was not skipped: " + skipped.message());
        }

        var reachable =
                DistributionalFunction1201.applyAll(
                        stone,
                        JsonParser.parseString(
                                "[{\"function\":\"example:unknown\","
                                    + "\"conditions\":[{\"condition\":\"minecraft:random_chance\",\"chance\":0.5}]}]"),
                        context,
                        100,
                        "/functions");
        if (reachable.supported() || !"/functions/0".equals(reachable.pointer())) {
            helper.fail("Reachable function did not fail at /functions/0: " + reachable);
        }

        var namespacedCollision =
                DistributionalFunction1201.applyAll(
                        stone,
                        JsonParser.parseString(
                                "[{\"function\":\"example:set_count\",\"count\":3}]"),
                        context,
                        100,
                        "/functions");
        if (namespacedCollision.supported()
                || !"/functions/0".equals(namespacedCollision.pointer())) {
            helper.fail("Non-Minecraft set_count was treated as a vanilla function");
        }

        var nonDamageable =
                DistributionalFunction1201.applyAll(
                        stone,
                        JsonParser.parseString(
                                "[{\"function\":\"minecraft:set_damage\","
                                        + "\"damage\":{\"type\":\"example:unknown\"}}]"),
                        context,
                        100,
                        "/functions");
        if (!nonDamageable.supported()) {
            helper.fail("Non-damageable stack parsed an unreachable provider");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void statefulFunctionsSkipOnlyUnreachableMechanisms(GameTestHelper helper) {
        LootAnalysisContext context =
                LootAnalysisContext.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F);
        XoroshiroState1201 initial = new XoroshiroState1201(13L, 21L);
        var skipped =
                StatefulFunction1201.applyAll(
                        new ItemStack(Items.STONE),
                        JsonParser.parseString(
                                "[{\"function\":\"minecraft:set_lore\","
                                    + "\"conditions\":[{\"condition\":\"minecraft:always_false\"}],"
                                    + "\"lore\":[{\"text\":\"unreachable\"}]}]"),
                        context,
                        initial,
                        "/functions");
        if (!skipped.supported()
                || !skipped.stack().is(Items.STONE)
                || !initial.equals(skipped.randomState())) {
            helper.fail("Unreachable stateful function changed the stack or random state");
        }

        var reachable =
                StatefulFunction1201.applyAll(
                        new ItemStack(Items.STONE),
                        JsonParser.parseString(
                                "[{\"function\":\"minecraft:set_lore\","
                                        + "\"lore\":[{\"text\":\"reachable\"}]}]"),
                        context,
                        initial,
                        "/functions");
        if (reachable.supported()
                || !"/functions/0".equals(reachable.pointer())
                || !initial.equals(reachable.randomState())) {
            helper.fail("Reachable stateful function did not fail without consuming randomness");
        }

        var namespacedCollision =
                StatefulFunction1201.applyAll(
                        new ItemStack(Items.STONE),
                        JsonParser.parseString(
                                "[{\"function\":\"example:set_count\",\"count\":3}]"),
                        context,
                        initial,
                        "/functions");
        if (namespacedCollision.supported()
                || !"/functions/0".equals(namespacedCollision.pointer())
                || !initial.equals(namespacedCollision.randomState())) {
            helper.fail("Stateful non-Minecraft set_count was treated as a vanilla function");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void unsupportedResultCannotExposePartialMeasure(GameTestHelper helper) {
        StackMeasure partial = new StackMeasure();
        partial.add(new StackState(new ItemStack(Items.STONE, 3)), ExactProbability.ONE);
        LootExpectationResult result =
                new LootExpectationResult(
                        AnalysisStatus.UNSUPPORTED,
                        partial,
                        java.util.List.of(
                                new Diagnostic("RANDOM_SEMANTICS", "unresolved continuation")));
        if (!result.measure().isEmpty()) {
            helper.fail("UNSUPPORTED result exposed a partial StackMeasure");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void distributionalFunctionOrderProducesExactFinalStackMeasure(
            GameTestHelper helper) {
        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/function_order");
        var result =
                DistributionalLootTableExecutor1201.evaluateLogicalCalls(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        1_000);
        if (!result.supported()) {
            helper.fail(
                    "Distributional function-order table was unsupported: " + result.diagnostics());
        }
        java.util.Map<Integer, ExactProbability> counts = new java.util.LinkedHashMap<>();
        result.measure().values().forEach((stack, mass) -> counts.put(stack.count(), mass));
        java.util.Map<Integer, ExactProbability> expected =
                java.util.Map.of(
                        6, ExactProbability.of(1, 4),
                        7, ExactProbability.of(1, 2),
                        8, ExactProbability.of(1, 4));
        if (!counts.equals(expected)) {
            helper.fail("Expected final count PMF " + expected + ", got " + counts);
        }
        if (!result.hasRandomCalls()) {
            helper.fail("Logical function-order analysis did not retain its random calls");
        }
        var production =
                DistributionalLootTableExecutor1201.evaluate(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        1_000);
        if (production.status() != AnalysisStatus.EXACT
                || !production.measure().values().equals(result.measure().values())) {
            helper.fail("Production analysis did not preserve the exact PMF: " + production);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void persistentRandomSequenceSnapshotMatchesRuntimeWithoutPreAdvance(
            GameTestHelper helper) {
        ResourceLocation sequenceId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/random_sequence_snapshot");
        var expected = PersistentRandomSequenceSnapshot1201.snapshot(helper.getLevel(), sequenceId);
        var runtime = helper.getLevel().getRandomSequence(sequenceId);

        var bounded = expected.nextInt(137);
        if (bounded.value() != runtime.nextInt(137)) {
            helper.fail("Snapshot bounded nextInt did not match persistent runtime sequence");
        }
        var floatDraw = bounded.state().nextFloat();
        if (Float.floatToRawIntBits(floatDraw.value())
                != Float.floatToRawIntBits(runtime.nextFloat())) {
            helper.fail("Snapshot nextFloat did not match persistent runtime sequence");
        }
        if (floatDraw.state().nextBoolean().value() != runtime.nextBoolean()) {
            helper.fail("Snapshot continuation did not match persistent runtime sequence");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 1200)
    public static void vanillaChestRuntimeCorpusHasExactConcreteTransitions(GameTestHelper helper) {
        if (!isVanillaLootRuntime()) {
            helper.succeed();
            return;
        }
        assertVanillaLootRuntime(helper);
        var server = helper.getLevel().getServer();
        RuntimeLootAstSource source = new RuntimeLootAstSource(server);
        var chestTables =
                server.getLootData().getKeys(LootDataType.TABLE).stream()
                        .filter(id -> id.getNamespace().equals("minecraft"))
                        .filter(id -> id.getPath().startsWith("chests/"))
                        .sorted(java.util.Comparator.comparing(Object::toString))
                        .toList();
        if (chestTables.size() != 43) {
            helper.fail("Expected 43 vanilla chest tables, got " + chestTables.size());
        }
        for (var id : chestTables) {
            DimensionTechMod.LOGGER.info("Checking concrete loot transition for {}", id);
            var ast = source.table(id);
            if (ast.isEmpty() || !ast.get().json().isJsonObject()) {
                helper.fail("Runtime table did not serialize to an object: " + id);
            }
            var tableJson = ast.get().json().getAsJsonObject();
            if (!tableJson.has("random_sequence")) {
                helper.fail("Vanilla chest table has no runtime random_sequence: " + id);
            }
            ResourceLocation sequence =
                    ResourceLocation.tryParse(tableJson.get("random_sequence").getAsString());
            if (sequence == null) helper.fail("Invalid random_sequence in " + id);
            var result =
                    StatefulLootTableExecutor1201.execute(
                            server,
                            id,
                            LootAnalysisContext.at(
                                    helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                            PersistentRandomSequenceSnapshot1201.snapshot(
                                    helper.getLevel(), sequence));
            if (!result.supported()) {
                helper.fail(
                        "Unsupported concrete transition for " + id + ": " + result.diagnostics());
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 12000)
    public static void vanillaChestRuntimeCorpusIsExactInProduction(GameTestHelper helper) {
        if (!isVanillaLootRuntime()) {
            helper.succeed();
            return;
        }
        assertVanillaLootRuntime(helper);
        var server = helper.getLevel().getServer();
        var chestTables =
                server.getLootData().getKeys(LootDataType.TABLE).stream()
                        .filter(id -> id.getNamespace().equals("minecraft"))
                        .filter(id -> id.getPath().startsWith("chests/"))
                        .sorted(java.util.Comparator.comparing(Object::toString))
                        .toList();
        if (chestTables.size() != 43) {
            helper.fail("Expected 43 vanilla chest tables, got " + chestTables.size());
        }
        LootAnalysisContext context =
                LootAnalysisContext.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F);
        LootExpectationResult endCityResult = null;
        for (ResourceLocation id : chestTables) {
            DimensionTechMod.LOGGER.info("Checking exact loot expectation for {}", id);
            var result =
                    DistributionalLootTableExecutor1201.evaluate(server, id, context, 1_000_000);
            if (result.status() != AnalysisStatus.EXACT) {
                helper.fail(
                        "Production exact analysis failed for " + id + ": " + result.diagnostics());
            }
            if (id.equals(BuiltInLootTables.END_CITY_TREASURE)) {
                endCityResult = result;
            }
        }
        if (endCityResult == null) {
            helper.fail("Vanilla chest corpus did not include end_city_treasure");
        }
        if (endCityResult.terminalMeasure().isEmpty()) {
            helper.fail("end_city_treasure produced an empty terminal measure");
        }
        ExactProbability rareItems =
                endCityResult
                        .terminalMeasure()
                        .exactItemCount(Items.DIAMOND)
                        .add(endCityResult.terminalMeasure().exactItemCount(Items.DIAMOND_SWORD));
        if (rareItems.isZero()) {
            helper.fail("end_city_treasure produced no diamond or diamond sword");
        }
        ExactProbability rarityValue =
                endCityResult
                        .terminalMeasure()
                        .exactRarityWeightedValue(
                                rarity ->
                                        switch (rarity) {
                                            case COMMON -> 1L;
                                            case UNCOMMON -> 10L;
                                            case RARE -> 50L;
                                            case EPIC -> 100L;
                                        });
        if (rarityValue.isZero() || !Double.isFinite(rarityValue.doubleValue())) {
            helper.fail(
                    "end_city_treasure rarity value was not finite and positive: " + rarityValue);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void enchantWithLevelsProducesNormalizedRuntimeStacks(GameTestHelper helper) {
        ItemStack input = new ItemStack(Items.FISHING_ROD);
        int enchantmentLevel = 5;
        int perturbationBound = input.getEnchantmentValue() / 4 + 1;
        var distribution =
                ExactEnchantmentSemantics1201.enchantItem(
                        input, enchantmentLevel, false, 1_000_000);
        ExactProbability total =
                distribution.masses().values().stream()
                        .reduce(ExactProbability.ZERO, ExactProbability::add);
        if (!total.equals(ExactProbability.ONE)) {
            helper.fail("Expected exact enchantment mass 1, got " + total);
        }
        boolean sawRuntimeEnchantment = false;
        for (var outcome : distribution.masses().keySet()) {
            ItemStack stack = outcome.value().stack();
            if (!stack.is(Items.FISHING_ROD)) {
                helper.fail("Expected enchanted fishing rod output, got " + stack);
            }
            sawRuntimeEnchantment |= !EnchantmentHelper.getEnchantments(stack).isEmpty();
            java.util.List<RandomCall> calls = outcome.calls();
            if (calls.size() < 4
                    || !calls.get(0)
                            .equals(new RandomCall(RandomMethod.NEXT_INT_BOUND, perturbationBound))
                    || !calls.get(1)
                            .equals(new RandomCall(RandomMethod.NEXT_INT_BOUND, perturbationBound))
                    || calls.get(2).method() != RandomMethod.NEXT_FLOAT
                    || calls.get(3).method() != RandomMethod.NEXT_FLOAT) {
                helper.fail("Unexpected enchant_with_levels call prefix: " + calls);
            }
        }
        if (!sawRuntimeEnchantment) {
            helper.fail("No enchant_with_levels branch produced a runtime-readable enchantment");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void randomFunctionHandlersUseSourceCallsAndOrder(GameTestHelper helper) {
        LootAnalysisContext context =
                LootAnalysisContext.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F);

        var damage =
                DistributionalFunction1201.applyAll(
                        new StackState(new ItemStack(Items.IRON_SWORD)),
                        JsonParser.parseString(
                                "[{\"function\":\"minecraft:set_damage\","
                                    + "\"damage\":{\"type\":\"minecraft:uniform\",\"min\":0.25,\"max\":0.75}}]"),
                        context,
                        1_000,
                        "/functions");
        assertFunctionCalls(
                helper,
                "set_damage",
                damage,
                java.util.List.of(new RandomCall(RandomMethod.NEXT_FLOAT, 0)));

        var randomEnchantment =
                DistributionalFunction1201.applyAll(
                        new StackState(new ItemStack(Items.BOOK)),
                        JsonParser.parseString(
                                "[{\"function\":\"minecraft:enchant_randomly\","
                                        + "\"enchantments\":[\"minecraft:swift_sneak\"]}]"),
                        context,
                        1_000,
                        "/functions");
        assertFunctionCalls(
                helper,
                "enchant_randomly",
                randomEnchantment,
                java.util.List.of(
                        new RandomCall(RandomMethod.NEXT_INT_BOUND, 1),
                        new RandomCall(RandomMethod.NEXT_INT_BOUND, 3)));

        var stew =
                DistributionalFunction1201.applyAll(
                        new StackState(new ItemStack(Items.SUSPICIOUS_STEW)),
                        JsonParser.parseString(
                                "[{\"function\":\"minecraft:set_stew_effect\",\"effects\":["
                                    + "{\"type\":\"minecraft:blindness\",\"duration\":{\"type\":\"minecraft:uniform\",\"min\":5,\"max\":6}},"
                                    + "{\"type\":\"minecraft:poison\",\"duration\":{\"type\":\"minecraft:uniform\",\"min\":7,\"max\":8}}]}]"),
                        context,
                        1_000,
                        "/functions");
        assertFunctionCalls(
                helper,
                "set_stew_effect",
                stew,
                java.util.List.of(
                        new RandomCall(RandomMethod.NEXT_INT_BOUND, 2),
                        new RandomCall(RandomMethod.NEXT_INT_BOUND, 2)));

        var instrument =
                DistributionalFunction1201.applyAll(
                        new StackState(new ItemStack(Items.GOAT_HORN)),
                        JsonParser.parseString(
                                "[{\"function\":\"minecraft:set_instrument\","
                                        + "\"options\":\"#minecraft:regular_goat_horns\"}]"),
                        context,
                        1_000,
                        "/functions");
        assertFunctionCalls(
                helper,
                "set_instrument",
                instrument,
                java.util.List.of(new RandomCall(RandomMethod.NEXT_INT_BOUND, 4)));
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void tableBonusWithoutToolUsesLevelZero(GameTestHelper helper) {
        LootAnalysisContext context =
                new LootAnalysisContext(
                        helper.getLevel(),
                        net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)),
                        null,
                        0.0F,
                        java.util.Map.of(),
                        null,
                        null,
                        null,
                        java.util.Map.of());
        var condition =
                JsonParser.parseString(
                        "{\"condition\":\"minecraft:table_bonus\","
                                + "\"enchantment\":\"minecraft:fortune\","
                                + "\"chances\":[0.25,1.0]}");

        var distributional =
                DistributionalCondition1201.test(condition, context, 100, "/condition", null);
        if (!distributional.supported()) {
            helper.fail("Missing-tool table_bonus was unsupported: " + distributional.message());
        }
        ExactProbability trueMass =
                distributional
                        .distribution()
                        .marginal()
                        .masses()
                        .getOrDefault(true, ExactProbability.ZERO);
        if (!trueMass.equals(ExactProbability.of(1, 4))) {
            helper.fail("Expected level-zero table_bonus chance 1/4, got " + trueMass);
        }
        boolean correctTrace =
                distributional.distribution().masses().keySet().stream()
                        .allMatch(
                                outcome ->
                                        outcome.calls()
                                                .equals(
                                                        java.util.List.of(
                                                                new RandomCall(
                                                                        RandomMethod.NEXT_FLOAT,
                                                                        0))));
        if (!correctTrace) {
            helper.fail("table_bonus did not use exactly one nextFloat call");
        }

        XoroshiroState1201 initial = XoroshiroState1201.fromSeed(0x1201L);
        var expectedDraw = initial.nextFloat();
        var stateful = StatefulCondition1201.test(condition, context, initial);
        if (stateful == null
                || stateful.value() != (expectedDraw.value() < 0.25F)
                || !stateful.randomState().equals(expectedDraw.state())) {
            helper.fail("Stateful missing-tool table_bonus did not match source behavior");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 200)
    public static void statefulSimpleDungeonMatchesRuntimeRawOutputAndContinuation(
            GameTestHelper helper) {
        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon");
        LootParams params =
                new LootParams.Builder(helper.getLevel())
                        .withParameter(
                                LootContextParams.ORIGIN,
                                net.minecraft.world.phys.Vec3.atCenterOf(
                                        helper.absolutePos(BlockPos.ZERO)))
                        .withLuck(0.0F)
                        .create(LootContextParamSets.CHEST);
        XoroshiroState1201 initial =
                PersistentRandomSequenceSnapshot1201.snapshot(helper.getLevel(), tableId);

        var exact =
                StatefulLootTableExecutor1201.execute(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        initial);
        if (!exact.supported()) {
            helper.fail("Stateful executor failed: " + exact.diagnostics());
        }

        LootContext runtimeContext = new LootContext.Builder(params).create(tableId);
        ArrayList<StackState> runtimeOutputs = new ArrayList<>();
        helper.getLevel()
                .getServer()
                .getLootData()
                .getLootTable(tableId)
                .getRandomItemsRaw(
                        runtimeContext, stack -> runtimeOutputs.add(new StackState(stack)));

        if (!runtimeOutputs.equals(exact.outputs())) {
            helper.fail(
                    "Raw output mismatch: runtime="
                            + runtimeOutputs
                            + ", exact="
                            + exact.outputs());
        }
        long runtimeNext = runtimeContext.getRandom().nextLong();
        long exactNext = exact.randomState().nextLong().value();
        if (runtimeNext != exactNext) {
            helper.fail(
                    "Random continuation mismatch: runtime="
                            + runtimeNext
                            + ", exact="
                            + exactNext);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void setCountClampsToMaximumStackSizeInsideTheFunction(GameTestHelper helper) {
        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/set_count_clamp");
        LootParams params =
                new LootParams.Builder(helper.getLevel())
                        .withParameter(
                                LootContextParams.ORIGIN,
                                net.minecraft.world.phys.Vec3.atCenterOf(
                                        helper.absolutePos(BlockPos.ZERO)))
                        .create(LootContextParamSets.CHEST);
        LootContext runtimeContext =
                new LootContext.Builder(params).withOptionalRandomSeed(0x1201L).create(tableId);
        ArrayList<StackState> runtimeOutputs = new ArrayList<>();
        helper.getLevel()
                .getServer()
                .getLootData()
                .getLootTable(tableId)
                .getRandomItemsRaw(
                        runtimeContext, stack -> runtimeOutputs.add(new StackState(stack)));

        XoroshiroState1201 initial = XoroshiroState1201.fromSeed(0x1201L);
        var exact =
                StatefulLootTableExecutor1201.execute(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        initial);
        if (!exact.supported()) helper.fail("set_count failed: " + exact.diagnostics());
        if (!runtimeOutputs.equals(exact.outputs())) {
            helper.fail(
                    "set_count output mismatch: runtime="
                            + runtimeOutputs
                            + ", exact="
                            + exact.outputs());
        }
        if (exact.outputs().size() != 1 || exact.outputs().get(0).count() != 64) {
            helper.fail("Expected source-level set_count clamp to 64, got " + exact.outputs());
        }
        if (!exact.randomState().equals(initial)) {
            helper.fail("Constant set_count unexpectedly consumed random state");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void missingNestedTableMatchesRuntimeEmptyOutput(GameTestHelper helper) {
        assertReferenceBehavior(helper, "missing_table", 0, "MISSING_REFERENCE", "TABLE");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void recursiveNestedTableMatchesRuntimeEmptyOutput(GameTestHelper helper) {
        assertReferenceBehavior(helper, "recursive_table", 0, "RECURSIVE_REFERENCE", "TABLE");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void missingPredicateMatchesRuntimeFalse(GameTestHelper helper) {
        assertReferenceBehavior(helper, "missing_predicate", 0, "MISSING_REFERENCE", "PREDICATE");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void recursivePredicateMatchesRuntimeFalse(GameTestHelper helper) {
        assertReferenceBehavior(
                helper, "recursive_predicate", 0, "RECURSIVE_REFERENCE", "PREDICATE");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void missingFunctionMatchesRuntimeIdentity(GameTestHelper helper) {
        assertReferenceBehavior(helper, "missing_function", 2, "MISSING_REFERENCE", "FUNCTION");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void recursiveFunctionRetainsPriorModification(GameTestHelper helper) {
        assertReferenceBehavior(helper, "recursive_function", 3, "RECURSIVE_REFERENCE", "FUNCTION");
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void scoreNumberProviderMatchesRuntimeTargetObjectiveAndScale(
            GameTestHelper helper) {
        var scoreboard = helper.getLevel().getScoreboard();
        var objective = scoreboard.getObjective("dt_loot_score");
        if (objective == null) {
            objective =
                    scoreboard.addObjective(
                            "dt_loot_score",
                            ObjectiveCriteria.DUMMY,
                            net.minecraft.network.chat.Component.literal("dt_loot_score"),
                            ObjectiveCriteria.RenderType.INTEGER);
        }
        scoreboard.getOrCreatePlayerScore("dimension_tech_gametest", objective).setScore(7);

        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/score_provider");
        LootParams params =
                new LootParams.Builder(helper.getLevel())
                        .withParameter(
                                LootContextParams.ORIGIN,
                                net.minecraft.world.phys.Vec3.atCenterOf(
                                        helper.absolutePos(BlockPos.ZERO)))
                        .create(LootContextParamSets.CHEST);
        LootContext runtimeContext =
                new LootContext.Builder(params).withOptionalRandomSeed(0x1201L).create(tableId);
        ArrayList<StackState> runtimeOutputs = new ArrayList<>();
        helper.getLevel()
                .getServer()
                .getLootData()
                .getLootTable(tableId)
                .getRandomItemsRaw(
                        runtimeContext, stack -> runtimeOutputs.add(new StackState(stack)));

        var exact =
                StatefulLootTableExecutor1201.execute(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        XoroshiroState1201.fromSeed(0x1201L));
        if (!exact.supported()) {
            helper.fail("Score provider execution failed: " + exact.diagnostics());
        }
        if (!runtimeOutputs.equals(exact.outputs())) {
            helper.fail(
                    "Score provider output mismatch: runtime="
                            + runtimeOutputs
                            + ", exact="
                            + exact.outputs());
        }
        if (exact.outputs().size() != 1 || exact.outputs().get(0).count() != 11) {
            helper.fail("Expected Math.round(7 * 1.5F) = 11, got " + exact.outputs());
        }
        var distributional =
                DistributionalLootTableExecutor1201.evaluateLogicalCalls(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        100);
        if (!distributional.supported()
                || distributional.measure().values().size() != 1
                || distributional.measure().values().keySet().iterator().next().count() != 11) {
            helper.fail(
                    "Distributional score provider mismatch: "
                            + distributional.measure().values()
                            + ", diagnostics="
                            + distributional.diagnostics());
        }
        if (distributional.hasRandomCalls()) {
            helper.fail("Deterministic score table unexpectedly consumed RandomSource");
        }
        var production =
                DistributionalLootTableExecutor1201.evaluate(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        100);
        if (production.status() != AnalysisStatus.EXACT
                || !production.measure().values().equals(distributional.measure().values())) {
            helper.fail("Deterministic score table was not certified EXACT: " + production);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void entryPoolAndTableFunctionsMatchRuntimeOrderAndContinuation(
            GameTestHelper helper) {
        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/function_order");
        LootParams params =
                new LootParams.Builder(helper.getLevel())
                        .withParameter(
                                LootContextParams.ORIGIN,
                                net.minecraft.world.phys.Vec3.atCenterOf(
                                        helper.absolutePos(BlockPos.ZERO)))
                        .create(LootContextParamSets.CHEST);
        XoroshiroState1201 initial =
                PersistentRandomSequenceSnapshot1201.snapshot(helper.getLevel(), tableId);
        var exact =
                StatefulLootTableExecutor1201.execute(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        initial);
        if (!exact.supported()) {
            helper.fail("Function order execution failed: " + exact.diagnostics());
        }

        LootContext runtimeContext = new LootContext.Builder(params).create(tableId);
        ArrayList<StackState> runtimeOutputs = new ArrayList<>();
        helper.getLevel()
                .getServer()
                .getLootData()
                .getLootTable(tableId)
                .getRandomItemsRaw(
                        runtimeContext, stack -> runtimeOutputs.add(new StackState(stack)));
        if (!runtimeOutputs.equals(exact.outputs())) {
            helper.fail(
                    "Function order output mismatch: runtime="
                            + runtimeOutputs
                            + ", exact="
                            + exact.outputs());
        }
        long runtimeNext = runtimeContext.getRandom().nextLong();
        long exactNext = exact.randomState().nextLong().value();
        if (runtimeNext != exactNext) {
            helper.fail(
                    "Function order continuation mismatch: runtime="
                            + runtimeNext
                            + ", exact="
                            + exactNext);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void unsupportedDiagnosticsPreserveRootAndNestedCallPaths(GameTestHelper helper) {
        ResourceLocation unsupported =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/unsupported_function");
        ResourceLocation nestedRoot =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/nested_unsupported_function");
        LootAnalysisContext context =
                LootAnalysisContext.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F);

        var direct =
                StatefulLootTableExecutor1201.execute(
                        helper.getLevel().getServer(),
                        unsupported,
                        context,
                        XoroshiroState1201.fromSeed(31L));
        assertUnsupportedDiagnostic(
                helper,
                direct,
                unsupported,
                "/pools/0/entries/0/functions/0",
                java.util.List.of(unsupported.toString()));

        var nested =
                StatefulLootTableExecutor1201.execute(
                        helper.getLevel().getServer(),
                        nestedRoot,
                        context,
                        XoroshiroState1201.fromSeed(47L));
        assertUnsupportedDiagnostic(
                helper,
                nested,
                unsupported,
                "/pools/0/entries/0/functions/0",
                java.util.List.of(nestedRoot.toString(), unsupported.toString()));
        helper.succeed();
    }

    private static void assertVanillaLootRuntime(GameTestHelper helper) {
        Set<String> expected = Set.of("minecraft", "forge", DimensionTechMod.MOD_ID);
        Set<String> loaded = new TreeSet<>();
        ModList.get().getMods().forEach(mod -> loaded.add(mod.getModId()));
        if (!loaded.equals(expected)) {
            helper.fail(
                    "Vanilla loot corpus requires only "
                            + new TreeSet<>(expected)
                            + ", loaded "
                            + loaded
                            + ". Run with -PvanillaLootRuntime=true.");
        }
    }

    private static boolean isVanillaLootRuntime() {
        return Boolean.getBoolean("dimension_tech.vanilla_loot_runtime");
    }

    private static void assertUnsupportedDiagnostic(
            GameTestHelper helper,
            StatefulLootTableExecutor1201.Result result,
            ResourceLocation tableId,
            String pointer,
            java.util.List<String> callPath) {
        if (result.supported()) {
            helper.fail("Expected unsupported result for " + tableId);
        }
        boolean found =
                result.diagnostics().stream()
                        .anyMatch(
                                diagnostic ->
                                        diagnostic.code().equals("UNSUPPORTED_TYPE")
                                                && tableId.equals(diagnostic.lootTableId())
                                                && pointer.equals(diagnostic.jsonPointer())
                                                && callPath.equals(diagnostic.callPath()));
        if (!found) {
            helper.fail(
                    "Missing structured unsupported diagnostic for "
                            + tableId
                            + ": "
                            + result.diagnostics());
        }
    }

    private static void assertFunctionCalls(
            GameTestHelper helper,
            String function,
            DistributionalFunction1201.Evaluation evaluation,
            java.util.List<RandomCall> expectedCalls) {
        if (!evaluation.supported()) {
            helper.fail(function + " was unsupported: " + evaluation.message());
        }
        if (evaluation.distribution().masses().isEmpty()) {
            helper.fail(function + " produced no reachable outcomes");
        }
        boolean allMatch =
                evaluation.distribution().masses().keySet().stream()
                        .allMatch(outcome -> outcome.calls().equals(expectedCalls));
        if (!allMatch) {
            helper.fail(
                    function
                            + " call traces did not all equal "
                            + expectedCalls
                            + ": "
                            + evaluation.distribution().masses().keySet().stream()
                                    .map(outcome -> outcome.calls().toString())
                                    .distinct()
                                    .toList());
        }
    }

    private static void assertReferenceBehavior(
            GameTestHelper helper,
            String tablePath,
            int expectedCount,
            String diagnosticCode,
            String referenceKind) {
        ResourceLocation tableId =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "gametest/" + tablePath);
        LootParams params =
                new LootParams.Builder(helper.getLevel())
                        .withParameter(
                                LootContextParams.ORIGIN,
                                net.minecraft.world.phys.Vec3.atCenterOf(
                                        helper.absolutePos(BlockPos.ZERO)))
                        .create(LootContextParamSets.CHEST);
        LootContext runtimeContext =
                new LootContext.Builder(params).withOptionalRandomSeed(0x1201L).create(tableId);
        ArrayList<StackState> runtimeOutputs = new ArrayList<>();
        helper.getLevel()
                .getServer()
                .getLootData()
                .getLootTable(tableId)
                .getRandomItemsRaw(
                        runtimeContext, stack -> runtimeOutputs.add(new StackState(stack)));

        var exact =
                StatefulLootTableExecutor1201.execute(
                        helper.getLevel().getServer(),
                        tableId,
                        LootAnalysisContext.at(
                                helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F),
                        XoroshiroState1201.fromSeed(0x1201L));
        if (!exact.supported()) {
            helper.fail(
                    "Stateful reference execution failed for "
                            + tableId
                            + ": "
                            + exact.diagnostics());
        }
        if (!runtimeOutputs.equals(exact.outputs())) {
            helper.fail(
                    "Reference output mismatch for "
                            + tableId
                            + ": runtime="
                            + runtimeOutputs
                            + ", exact="
                            + exact.outputs());
        }
        int actualCount = exact.outputs().stream().mapToInt(StackState::count).sum();
        if (actualCount != expectedCount) {
            helper.fail(
                    "Expected count " + expectedCount + " for " + tableId + ", got " + actualCount);
        }
        boolean hasDiagnostic =
                exact.diagnostics().stream()
                        .anyMatch(
                                diagnostic ->
                                        diagnostic.code().equals(diagnosticCode)
                                                && diagnostic
                                                        .message()
                                                        .startsWith(referenceKind + " "));
        if (!hasDiagnostic) {
            helper.fail(
                    "Missing "
                            + diagnosticCode
                            + " "
                            + referenceKind
                            + " diagnostic for "
                            + tableId
                            + ": "
                            + exact.diagnostics());
        }

        LootAnalysisContext analysisContext =
                LootAnalysisContext.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F);
        var distributional =
                DistributionalLootTableExecutor1201.evaluateLogicalCalls(
                        helper.getLevel().getServer(), tableId, analysisContext, 1_000);
        if (!distributional.supported()) {
            helper.fail(
                    "Distributional reference execution failed for "
                            + tableId
                            + ": "
                            + distributional.diagnostics());
        }
        StackMeasure runtimeMeasure = new StackMeasure();
        runtimeOutputs.forEach(stack -> runtimeMeasure.add(stack, ExactProbability.ONE));
        if (!runtimeMeasure.values().equals(distributional.measure().values())) {
            helper.fail(
                    "Distributional reference output mismatch for "
                            + tableId
                            + ": runtime="
                            + runtimeMeasure.values()
                            + ", distributional="
                            + distributional.measure().values());
        }
        boolean distributionalHasDiagnostic =
                distributional.diagnostics().stream()
                        .anyMatch(
                                diagnostic ->
                                        diagnostic.code().equals(diagnosticCode)
                                                && diagnostic
                                                        .message()
                                                        .startsWith(referenceKind + " "));
        if (!distributionalHasDiagnostic) {
            helper.fail(
                    "Missing distributional "
                            + diagnosticCode
                            + " "
                            + referenceKind
                            + " diagnostic for "
                            + tableId
                            + ": "
                            + distributional.diagnostics());
        }
    }
}
