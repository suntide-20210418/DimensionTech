package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.loot.expectation.LootExpectationResult;
import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class LootExpectationResultTest {
    @Test
    void diagnosticsAreDefensivelyCopied() {
        ArrayList<Diagnostic> diagnostics = new ArrayList<>();
        diagnostics.add(new Diagnostic("WARNING", "first"));
        LootExpectationResult result =
                new LootExpectationResult(AnalysisStatus.EXACT, new StackMeasure(), diagnostics);

        diagnostics.add(new Diagnostic("WARNING", "second"));

        assertEquals(1, result.diagnostics().size());
        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    }
}
