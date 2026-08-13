package com.suntide_20210418.dimensiontech.gametest;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactEnchantmentSemantics1201;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.JsonLootTableExecutor;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.LootAnalysisContext;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.PersistentRandomSequenceSnapshot1201;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.RuntimeLootAstSource;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LootExpectationGameTests {
    private LootExpectationGameTests() {}

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
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void persistentRandomSequenceSnapshotMatchesRuntimeWithoutPreAdvance(
            GameTestHelper helper) {
        ResourceLocation sequenceId = ResourceLocation.fromNamespaceAndPath(
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

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 200)
    public static void vanillaChestRuntimeCorpusIsAnalyzedWithoutFalseExactResults(
            GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        RuntimeLootAstSource source = new RuntimeLootAstSource(server);
        var chestTables = server.getLootData().getKeys(LootDataType.TABLE).stream()
                .filter(id -> id.getNamespace().equals("minecraft"))
                .filter(id -> id.getPath().startsWith("chests/"))
                .sorted(java.util.Comparator.comparing(Object::toString))
                .toList();
        if (chestTables.size() != 43) {
            helper.fail("Expected 43 vanilla chest tables, got " + chestTables.size());
        }
        for (var id : chestTables) {
            var ast = source.table(id);
            if (ast.isEmpty() || !ast.get().json().isJsonObject()) {
                helper.fail("Runtime table did not serialize to an object: " + id);
            }
            var result = JsonLootTableExecutor.evaluate(
                    server,
                    id,
                    LootAnalysisContext.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), 0.0F));
            if (result.status() == AnalysisStatus.EXACT) {
                helper.fail("Persistent random sequence was incorrectly certified EXACT: " + id);
            }
            boolean hasStructuredRandomSequenceDiagnostic = result.diagnostics().stream()
                    .anyMatch(diagnostic -> diagnostic.code().equals("RANDOM_SEMANTICS")
                            && id.equals(diagnostic.lootTableId())
                            && diagnostic.jsonPointer().equals("/random_sequence")
                            && diagnostic.callPath().equals(java.util.List.of(id.toString())));
            if (!hasStructuredRandomSequenceDiagnostic) {
                helper.fail("Missing structured random_sequence diagnostic: " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty", timeoutTicks = 400)
    public static void enchantWithLevelsProducesNormalizedRuntimeStacks(GameTestHelper helper) {
        var distribution = ExactEnchantmentSemantics1201.enchantItem(
                new ItemStack(Items.IRON_SWORD), 1, false, 1_000_000);
        ExactProbability total = distribution.masses().values().stream()
                .reduce(ExactProbability.ZERO, ExactProbability::add);
        if (!total.equals(ExactProbability.ONE)) {
            helper.fail("Expected exact enchantment mass 1, got " + total);
        }
        for (var outcome : distribution.masses().keySet()) {
            ItemStack stack = outcome.value().stack();
            if (!stack.is(Items.IRON_SWORD)) {
                helper.fail("Expected enchanted iron sword output, got " + stack);
            }
            if (EnchantmentHelper.getEnchantments(stack).isEmpty()) {
                helper.fail("Enchantment output had no runtime-readable enchantments: " + stack);
            }
        }
        helper.succeed();
    }
}
