package com.suntide_20210418.dimensiontech.utils.loot.expectation;

import java.util.List;
import java.util.Objects;

public record LootExpectationResult(
        AnalysisStatus status,
        StackMeasure measure,
        TerminalStackMeasure terminalMeasure,
        boolean fullStackMeasureAvailable,
        List<Diagnostic> diagnostics) {
    public LootExpectationResult {
        status = Objects.requireNonNull(status, "status");
        measure = Objects.requireNonNull(measure, "measure");
        terminalMeasure = Objects.requireNonNull(terminalMeasure, "terminalMeasure");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (status != AnalysisStatus.EXACT) {
            measure = new StackMeasure();
            terminalMeasure = TerminalStackMeasure.empty();
            fullStackMeasureAvailable = false;
        }
    }

    public LootExpectationResult(
            AnalysisStatus status, StackMeasure measure, List<Diagnostic> diagnostics) {
        this(
                status,
                measure,
                status == AnalysisStatus.EXACT
                        ? TerminalStackMeasure.from(measure)
                        : TerminalStackMeasure.empty(),
                status == AnalysisStatus.EXACT,
                diagnostics);
    }

    public static LootExpectationResult exactTerminal(
            TerminalStackMeasure terminalMeasure, List<Diagnostic> diagnostics) {
        return new LootExpectationResult(
                AnalysisStatus.EXACT, new StackMeasure(), terminalMeasure, false, diagnostics);
    }

    public StackMeasure requireFullStackMeasure() {
        if (!fullStackMeasureAvailable) {
            throw new IllegalStateException(
                    "The exact result was terminally aggregated; a full StackMeasure is not"
                            + " materialized");
        }
        return measure;
    }

    public static LootExpectationResult unsupported(String message) {
        return new LootExpectationResult(
                AnalysisStatus.UNSUPPORTED,
                new StackMeasure(),
                List.of(new Diagnostic("UNSUPPORTED_TYPE", message)));
    }

    public static LootExpectationResult unsupported(Diagnostic diagnostic) {
        return new LootExpectationResult(
                AnalysisStatus.UNSUPPORTED, new StackMeasure(), List.of(diagnostic));
    }
}
