package com.suntide_20210418.dimensiontech.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.DiscoveryResult;
import com.suntide_20210418.dimensiontech.utils.StructureLootAnalyzer.StructureLoot;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackMeasure;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

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
