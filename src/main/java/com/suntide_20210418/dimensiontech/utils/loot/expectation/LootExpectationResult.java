package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.List;

public record LootExpectationResult(AnalysisStatus status, StackMeasure measure, List<Diagnostic> diagnostics) {
    public static LootExpectationResult unsupported(String message) {
        return new LootExpectationResult(AnalysisStatus.UNSUPPORTED, new StackMeasure(),
                List.of(new Diagnostic("UNSUPPORTED_TYPE", message)));
    }
}
