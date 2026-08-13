package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ReferenceSemantics1201Test {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("dimension_tech", "gametest/reference");

    @Test
    void missingAndRecursiveTablesProduceExactEmptyOutputWithWarnings() {
        LootExpectationResult missing = ReferenceSemantics1201.emptyTable(
                ReferenceSemantics1201.missingTable(ID));
        LootExpectationResult recursive = ReferenceSemantics1201.emptyTable(
                ReferenceSemantics1201.recursiveTable(ID));

        assertEquals(AnalysisStatus.EXACT, missing.status());
        assertTrue(missing.measure().isEmpty());
        assertEquals("MISSING_REFERENCE", missing.diagnostics().get(0).code());
        assertEquals(AnalysisStatus.EXACT, recursive.status());
        assertTrue(recursive.measure().isEmpty());
        assertEquals("RECURSIVE_REFERENCE", recursive.diagnostics().get(0).code());
    }

    @Test
    void predicateFallbacksAreFalseAndFunctionFallbacksAreIdentityDiagnostics() {
        assertEquals("false", suffix(ReferenceSemantics1201.missingPredicate(ID)));
        assertEquals("false", suffix(ReferenceSemantics1201.recursivePredicate(ID)));
        assertEquals("identity", suffix(ReferenceSemantics1201.missingFunction(ID)));
        assertEquals("identity", suffix(ReferenceSemantics1201.recursiveFunction(ID)));
    }

    private static String suffix(Diagnostic diagnostic) {
        String message = diagnostic.message();
        int resolvedAs = message.indexOf("resolved as ") + "resolved as ".length();
        return message.substring(resolvedAs, message.indexOf(" (1.20.1)"));
    }
}
