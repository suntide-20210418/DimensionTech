package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.ExactProbability;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable expectation-layer output, including optional complete stack data for legacy adapters. */
public record LootExpectationSnapshot(AnalysisStatus status,
        StructureValueSnapshot.Expectation terminal,
        Map<StackData, ExactProbability> stacks,
        boolean fullStackMeasureAvailable,
        List<Diagnostic> diagnostics) {
    public LootExpectationSnapshot {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(terminal, "terminal");
        stacks = Map.copyOf(stacks);
        diagnostics = List.copyOf(diagnostics);
    }

    /** NBT is serialized on the runtime side; no mutable tag is retained by a background task. */
    public record StackData(String itemId, int count, String serializedNbt) {
        public StackData {
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(serializedNbt, "serializedNbt");
        }
    }
}
