package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkedStructure;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.RuntimeLootAstSource;
import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.loot.expectation.StackState;
import com.suntide_20210418.dimensiontech.loot.expectation.TerminalStackKey;
import com.suntide_20210418.dimensiontech.loot.expectation.TerminalStackMeasure;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.StructureLoot;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator.StructureValue;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime-registry tests for value conversion and terminal loot projection. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureValueCalculatorGameTests {
    private StructureValueCalculatorGameTests() {}

    @GameTest(
            templateNamespace = "minecraft",
            template = "empty",
            batch = "structure_discovery_sharing",
            timeoutTicks = 200)
    public static void discoveryRequestsShareTheServerFuture(GameTestHelper helper) {
        ResourceLocation structure =
                ResourceLocation.fromNamespaceAndPath("minecraft", "desert_pyramid");
        StructureAnalysisService service =
                StructureAnalysisService.forServer(helper.getLevel().getServer());
        CompletableFuture<DiscoveryResult> first = service.discover(helper.getLevel(), structure);
        CompletableFuture<DiscoveryResult> second = service.discover(helper.getLevel(), structure);
        if (first != second) {
            helper.fail("Identical discovery requests did not share one Future");
        }
        helper.succeedWhen(
                () -> {
                    if (!first.isDone()) helper.fail("Shared discovery has not completed");
                    DiscoveryResult result = first.join();
                    if (result.status() != AnalysisStatus.EXACT
                            && result.status() != AnalysisStatus.APPROXIMATE) {
                        helper.fail("Shared discovery did not produce a usable result: " + result);
                    }
                });
    }

    @GameTest(
            templateNamespace = "minecraft",
            template = "empty",
            batch = "layered_value_equivalence",
            timeoutTicks = 200)
    public static void layeredAsyncResultMatchesSynchronousResultItemByItem(GameTestHelper helper) {
        ResourceLocation table =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "test/layered_equivalence");
        ResourceLocation structure =
                ResourceLocation.fromNamespaceAndPath(
                        DimensionTechMod.MOD_ID, "layered_equivalence");
        MarkerInfo marker =
                new MarkerInfo(
                        helper.getLevel().dimension().location(),
                        helper.absolutePos(BlockPos.ZERO),
                        new MarkedStructure(structure, new BoundingBox(0, 0, 0, 0, 0, 0)));
        DiscoveryResult discovery =
                new DiscoveryResult(
                        AnalysisStatus.EXACT,
                        List.of(new StructureLoot(structure, List.of(table), List.of(), List.of())),
                        List.of());

        StructureValue synchronous =
                StructureValueCalculator.calculate(helper.getLevel(), marker, 0.0F, discovery);
        RuntimeLootAstSource source =
                RuntimeLootAstSource.snapshotTables(helper.getLevel().getServer(), List.of(table));
        StructureValue invalidLuck =
                StructureValueCalculator.calculateAsync(
                                helper.getLevel().getServer(), marker, Float.NaN, discovery, source)
                        .join();
        if (invalidLuck.status() != AnalysisStatus.UNSUPPORTED
                || invalidLuck.diagnostics().stream()
                        .noneMatch(diagnostic -> "VALUE_SEMANTICS".equals(diagnostic.code()))) {
            helper.fail("Layered analysis accepted non-finite luck: " + invalidLuck);
        }
        CompletableFuture<StructureValue> asynchronous =
                StructureValueCalculator.calculateAsync(
                        helper.getLevel().getServer(), marker, 0.0F, discovery, source);

        helper.succeedWhen(
                () -> {
                    if (!asynchronous.isDone()) {
                        helper.fail("Layered analysis has not completed");
                    }
                    StructureValue actual = asynchronous.join();
                    if (actual.status() != synchronous.status()
                            || Double.compare(actual.structureValue(), synchronous.structureValue())
                                    != 0
                            || actual.fullStackMeasureAvailable()
                                    != synchronous.fullStackMeasureAvailable()
                            || !actual.measure().values().equals(synchronous.measure().values())
                            || !actual.terminalMeasure()
                                    .values()
                                    .equals(synchronous.terminalMeasure().values())) {
                        helper.fail(
                                "Layered analysis differs from synchronous analysis: synchronous="
                                        + synchronous
                                        + ", asynchronous="
                                        + actual);
                    }
                });
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void unavailableFullMeasureKeepsTerminalProjection(GameTestHelper helper) {
        StackMeasure partial = new StackMeasure();
        partial.add(new StackState(new ItemStack(Items.STONE)), ExactProbability.ONE);
        StructureValue value =
                new StructureValue(
                        AnalysisStatus.EXACT,
                        1.0D,
                        1.0D,
                        0.0F,
                        partial,
                        TerminalStackMeasure.from(partial),
                        false,
                        java.util.List.of());
        if (value.fullStackMeasureAvailable()
                || !value.measure().isEmpty()
                || !ExactProbability.ONE.equals(
                        value.terminalMeasure().exactItemCount(Items.STONE))) {
            helper.fail(
                    "Unavailable full measure leaked or lost its terminal projection: " + value);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void terminalValueStaysExactUntilDoubleBoundary(GameTestHelper helper) {
        TerminalStackKey key = TerminalStackKey.from(new ItemStack(Items.STONE, 3));
        ExactProbability ratio =
                ExactProbability.of(
                        BigInteger.ONE.shiftLeft(2_000).add(BigInteger.ONE),
                        BigInteger.ONE.shiftLeft(2_000));
        TerminalStackMeasure terminal = TerminalStackMeasure.of(Map.of(key, ratio));
        if (!ratio.multiply(ExactProbability.of(30, 1))
                        .equals(terminal.exactRarityWeightedValueFromDouble(rarity -> 10.0D))
                || terminal.rarityWeightedValueAsDouble(rarity -> 10.0D) != 30.0D) {
            helper.fail("Terminal value crossed the double boundary too early: " + terminal);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void terminalValueOverflowIsUnsupported(GameTestHelper helper) {
        TerminalStackKey key = TerminalStackKey.from(new ItemStack(Items.STONE));
        TerminalStackMeasure terminal =
                TerminalStackMeasure.of(
                        Map.of(
                                key,
                                ExactProbability.of(
                                        BigInteger.ONE.shiftLeft(1024), BigInteger.ONE)));
        try {
            terminal.rarityWeightedValueAsDouble(rarity -> 1.0D);
            helper.fail("Terminal value overflow was silently converted to a double");
            return;
        } catch (ArithmeticException expected) {
            // The exact terminal measure may be unbounded at the persistence boundary.
        }
        StructureValueCalculator.TerminalValueEvaluation evaluation =
                StructureValueCalculator.evaluateTerminalValue(terminal, rarity -> 1.0D, 1.0D);
        if (evaluation.supported()
                || evaluation.value() != 0.0D
                || !"VALUE_SEMANTICS".equals(evaluation.diagnostic().code())
                || !"Exact structure value cannot be represented as a finite non-negative double: exact value does not fit in a finite double"
                        .equals(evaluation.diagnostic().message())) {
            helper.fail("Terminal value overflow had an unstable result: " + evaluation);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void nonFiniteDimensionValueIsUnsupported(GameTestHelper helper) {
        StructureValueCalculator.TerminalValueEvaluation evaluation =
                StructureValueCalculator.evaluateTerminalValue(
                        TerminalStackMeasure.empty(), rarity -> 1.0D, Double.POSITIVE_INFINITY);
        if (evaluation.supported() || !"VALUE_SEMANTICS".equals(evaluation.diagnostic().code())) {
            helper.fail("Non-finite dimension value was accepted: " + evaluation);
        }
        helper.succeed();
    }
}
