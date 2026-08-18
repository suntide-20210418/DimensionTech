package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackState;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.TerminalStackKey;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.TerminalStackMeasure;
import java.math.BigInteger;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime-registry tests for value conversion and terminal loot projection. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureValueCalculatorGameTests {
    private StructureValueCalculatorGameTests() {}

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
