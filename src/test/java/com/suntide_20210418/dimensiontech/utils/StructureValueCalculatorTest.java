package com.suntide_20210418.dimensiontech.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.StructureLoot;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackState;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.TerminalStackKey;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.TerminalStackMeasure;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

class StructureValueCalculatorTest {
    @Test
    void rootTablesAreDistinctWithinEachStructureButRetainedAcrossStructures() {
        ResourceLocation shared = new ResourceLocation("test", "shared");
        ResourceLocation firstOnly = new ResourceLocation("test", "first_only");
        DiscoveryResult discovery =
                new DiscoveryResult(
                        AnalysisStatus.EXACT,
                        List.of(
                                structure("first", List.of(shared, firstOnly, shared, firstOnly)),
                                structure("second", List.of(shared, shared))),
                        List.of());

        assertEquals(
                List.of(firstOnly, shared, shared),
                StructureValueCalculator.rootTablesForValue(discovery));

        DiscoveryResult unsupported =
                new DiscoveryResult(AnalysisStatus.UNSUPPORTED, discovery.structures(), List.of());
        assertEquals(List.of(), StructureValueCalculator.rootTablesForValue(unsupported));
    }

    @Test
    void unavailableFullMeasureNeverExposesAPlaceDependentPartialMeasure() {
        StackMeasure partial = new StackMeasure();
        partial.add(new StackState(new ItemStack(Items.STONE)), ExactProbability.ONE);
        TerminalStackMeasure terminal = TerminalStackMeasure.from(partial);

        StructureValue value =
                new StructureValue(
                        AnalysisStatus.EXACT,
                        1.0D,
                        1.0D,
                        0.0F,
                        partial,
                        terminal,
                        false,
                        List.of());

        assertFalse(value.fullStackMeasureAvailable());
        assertTrue(value.measure().isEmpty());
        assertEquals(ExactProbability.ONE, value.terminalMeasure().exactItemCount(Items.STONE));
    }

    @Test
    void terminalValueUsesExactMassBeforeTheDoubleBoundary() {
        TerminalStackKey key = TerminalStackKey.from(new ItemStack(Items.STONE, 3));
        ExactProbability hugeFiniteRatio =
                ExactProbability.of(
                        BigInteger.ONE.shiftLeft(2_000).add(BigInteger.ONE),
                        BigInteger.ONE.shiftLeft(2_000));
        TerminalStackMeasure terminal = TerminalStackMeasure.of(Map.of(key, hugeFiniteRatio));

        assertEquals(
                hugeFiniteRatio.multiply(ExactProbability.of(30, 1)),
                terminal.exactRarityWeightedValueFromDouble(rarity -> 10.0D));
        assertEquals(30.0D, terminal.rarityWeightedValueAsDouble(rarity -> 10.0D));
    }

    @Test
    void terminalValueOverflowIsNotSilentlySavedAsInfinity() {
        TerminalStackKey key = TerminalStackKey.from(new ItemStack(Items.STONE));
        TerminalStackMeasure terminal =
                TerminalStackMeasure.of(
                        Map.of(
                                key,
                                ExactProbability.of(
                                        BigInteger.ONE.shiftLeft(1024), BigInteger.ONE)));

        assertThrows(
                ArithmeticException.class,
                () -> terminal.rarityWeightedValueAsDouble(rarity -> 1.0D));
    }

    @Test
    void structureValueReportsOverflowAsUnsupportedWithAStableDiagnostic() {
        TerminalStackKey key = TerminalStackKey.from(new ItemStack(Items.STONE));
        TerminalStackMeasure terminal =
                TerminalStackMeasure.of(
                        Map.of(
                                key,
                                ExactProbability.of(
                                        BigInteger.ONE.shiftLeft(1024), BigInteger.ONE)));

        StructureValueCalculator.TerminalValueEvaluation evaluation =
                StructureValueCalculator.evaluateTerminalValue(terminal, rarity -> 1.0D, 1.0D);

        assertFalse(evaluation.supported());
        assertEquals(0.0D, evaluation.value());
        assertEquals("VALUE_SEMANTICS", evaluation.diagnostic().code());
        assertEquals(
                "Exact structure value cannot be represented as a finite non-negative double:"
                        + " exact value does not fit in a finite double",
                evaluation.diagnostic().message());
    }

    @Test
    void structureValueRejectsNonFiniteDimensionAtTheValueBoundary() {
        StructureValueCalculator.TerminalValueEvaluation evaluation =
                StructureValueCalculator.evaluateTerminalValue(
                        TerminalStackMeasure.empty(), rarity -> 1.0D, Double.POSITIVE_INFINITY);

        assertFalse(evaluation.supported());
        assertEquals("VALUE_SEMANTICS", evaluation.diagnostic().code());
    }

    @Test
    void exactStructureValueRejectsNonFiniteTerminalValue() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StructureValue(
                                AnalysisStatus.EXACT,
                                1.0D,
                                Double.POSITIVE_INFINITY,
                                0.0F,
                                new StackMeasure(),
                                List.of()));
    }

    private static StructureLoot structure(String id, List<ResourceLocation> roots) {
        return new StructureLoot(new ResourceLocation("test", id), roots, List.of(), List.of());
    }
}
