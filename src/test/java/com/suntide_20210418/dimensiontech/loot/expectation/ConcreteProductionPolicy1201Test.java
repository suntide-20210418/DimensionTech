package com.suntide_20210418.dimensiontech.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

class ConcreteProductionPolicy1201Test {
    private static final ResourceLocation TABLE_ID =
            ResourceLocation.fromNamespaceAndPath("dimension_tech", "policy");

    @Test
    void reachableRandomCallsCannotBeReportedAsConcreteExact() {
        var result =
                DistributionalLootTableExecutor1201.concreteProductionResult(
                        TABLE_ID, logicalResult(true));

        assertEquals(AnalysisStatus.UNSUPPORTED, result.status());
        assertTrue(result.measure().isEmpty());
        assertEquals(1, result.diagnostics().size());
        Diagnostic diagnostic = result.diagnostics().get(0);
        assertEquals("RANDOM_SEMANTICS", diagnostic.code());
        assertEquals(TABLE_ID, diagnostic.lootTableId());
        assertEquals(List.of(TABLE_ID.toString()), diagnostic.callPath());
    }

    @Test
    void deterministicResultRemainsExact() {
        var result =
                DistributionalLootTableExecutor1201.concreteProductionResult(
                        TABLE_ID, logicalResult(false));

        assertEquals(AnalysisStatus.EXACT, result.status());
        assertTrue(result.measure().isEmpty());
        assertTrue(result.diagnostics().isEmpty());
    }

    private static DistributionalLootTableExecutor1201.LogicalResult logicalResult(
            boolean hasRandomCalls) {
        return new DistributionalLootTableExecutor1201.LogicalResult(
                true,
                new StackMeasure(),
                TerminalStackMeasure.empty(),
                true,
                hasRandomCalls,
                List.of());
    }
}
