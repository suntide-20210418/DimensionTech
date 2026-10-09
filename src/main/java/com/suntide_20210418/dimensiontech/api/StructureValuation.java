package com.suntide_20210418.dimensiontech.api;

import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * A single structure's (or chest's) valuation result, flattened for external consumers.
 *
 * <p>The three figures are exactly the ones DimensionTech shows on its own marker tooltips:
 *
 * <ul>
 *   <li>{@link #dimensionValue()} — the dimension multiplier from the {@code structureValue}
 *       config, already folded into the analysis.
 *   <li>{@link #structureValue()} — the compressed loot expectation ({@code sqrt(raw) * 50}).
 *   <li>{@link #expectations()} — the per-item expected counts, descending. Only populated when
 *       {@link #usable()} is true.
 * </ul>
 *
 * <p>An {@code UNSUPPORTED} / {@code LEGACY} result carries no trustworthy numbers; consumers must
 * check {@link #usable()} before displaying values rather than rendering a misleading zero.
 */
public record StructureValuation(
        AnalysisStatus status,
        double dimensionValue,
        double structureValue,
        List<ItemExpectation> expectations) {

    public StructureValuation {
        expectations = List.copyOf(expectations);
    }

    /** Whether the numbers are safe to display ({@code EXACT} or {@code APPROXIMATE}). */
    public boolean usable() {
        return status == AnalysisStatus.EXACT || status == AnalysisStatus.APPROXIMATE;
    }

    /** Expected count of one item, as an average per-structure yield. */
    public record ItemExpectation(ResourceLocation itemId, double count) {}
}
