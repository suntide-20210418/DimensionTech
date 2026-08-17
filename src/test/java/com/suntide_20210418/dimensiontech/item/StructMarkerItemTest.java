package com.suntide_20210418.dimensiontech.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.IdealRandomProbabilitySpace1201;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackMeasure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

class StructMarkerItemTest {
    @Test
    void exactAnalysisWritesVersionLuckValueAndStableDiagnosticPayload() {
        CompoundTag marker = new CompoundTag();
        Diagnostic diagnostic =
                new Diagnostic(
                        "WARNING",
                        "stable warning",
                        new ResourceLocation("test", "root"),
                        "/pools/0",
                        List.of("test:root", "test:nested"));
        StructureValue value =
                new StructureValue(
                        AnalysisStatus.EXACT,
                        10.0D,
                        42.5D,
                        1.25F,
                        new StackMeasure(),
                        List.of(diagnostic));

        StructMarkerItem.writeAnalysisResult(marker, value);

        assertTrue(marker.contains("StructureValueAlgorithmVersion", Tag.TAG_ANY_NUMERIC));
        assertEquals(4, marker.getInt("StructureValueAlgorithmVersion"));
        assertEquals(AnalysisStatus.EXACT, StructMarkerItem.analysisStatus(marker));
        assertEquals(10.0D, marker.getDouble("DimensionValue"));
        assertEquals(42.5D, marker.getDouble("StructureValue"));
        assertEquals(1.25F, marker.getFloat("AnalysisLuck"));
        assertEquals(
                IdealRandomProbabilitySpace1201.ID, marker.getString("RandomProbabilitySpace"));

        ListTag diagnostics = marker.getList("AnalysisDiagnostics", Tag.TAG_COMPOUND);
        assertEquals(1, diagnostics.size());
        CompoundTag saved = diagnostics.getCompound(0);
        assertEquals("WARNING", saved.getString("Code"));
        assertEquals("stable warning", saved.getString("Message"));
        assertEquals("test:root", saved.getString("LootTable"));
        assertEquals("/pools/0", saved.getString("JsonPointer"));
        assertEquals(2, saved.getList("CallPath", Tag.TAG_STRING).size());
    }

    @Test
    void unsupportedAnalysisRemovesAnyPreviouslyReadableValue() {
        CompoundTag marker = new CompoundTag();
        marker.putDouble("StructureValue", 999.0D);
        StructureValue value =
                new StructureValue(
                        AnalysisStatus.UNSUPPORTED,
                        20.0D,
                        999.0D,
                        0.0F,
                        new StackMeasure(),
                        List.of(new Diagnostic("UNSUPPORTED_TYPE", "reachable mechanism")));

        StructMarkerItem.writeAnalysisResult(marker, value);

        assertEquals(AnalysisStatus.UNSUPPORTED, StructMarkerItem.analysisStatus(marker));
        assertFalse(marker.contains("StructureValue", Tag.TAG_ANY_NUMERIC));
    }

    @Test
    void unversionedAndOlderMarkersAreLegacy() {
        CompoundTag marker = new CompoundTag();
        marker.putString("AnalysisStatus", AnalysisStatus.EXACT.name());
        marker.putDouble("StructureValue", 999.0D);
        assertEquals(AnalysisStatus.LEGACY, StructMarkerItem.analysisStatus(marker));

        marker.putInt("StructureValueAlgorithmVersion", -1);
        assertEquals(AnalysisStatus.LEGACY, StructMarkerItem.analysisStatus(marker));

        marker.putInt("StructureValueAlgorithmVersion", 3);
        assertEquals(AnalysisStatus.LEGACY, StructMarkerItem.analysisStatus(marker));
    }

    @Test
    void incompleteCurrentPayloadCannotExposeAnExactStatus() {
        CompoundTag marker = new CompoundTag();
        StructureValue value =
                new StructureValue(
                        AnalysisStatus.EXACT, 1.0D, 2.0D, 0.0F, new StackMeasure(), List.of());
        StructMarkerItem.writeAnalysisResult(marker, value);

        marker.remove("RandomProbabilitySpace");
        assertEquals(AnalysisStatus.UNSUPPORTED, StructMarkerItem.analysisStatus(marker));

        marker.putString("RandomProbabilitySpace", IdealRandomProbabilitySpace1201.ID);
        marker.remove("StructureValue");
        assertEquals(AnalysisStatus.UNSUPPORTED, StructMarkerItem.analysisStatus(marker));
    }

    @Test
    void currentPayloadRequiresFiniteLuckDimensionValueAndDiagnostics() {
        CompoundTag marker = exactMarker();

        marker.remove("AnalysisLuck");
        assertEquals(AnalysisStatus.UNSUPPORTED, StructMarkerItem.analysisStatus(marker));

        marker = exactMarker();
        marker.remove("DimensionValue");
        assertEquals(AnalysisStatus.UNSUPPORTED, StructMarkerItem.analysisStatus(marker));

        marker = exactMarker();
        marker.remove("AnalysisDiagnostics");
        assertEquals(AnalysisStatus.UNSUPPORTED, StructMarkerItem.analysisStatus(marker));

        marker = exactMarker();
        marker.putDouble("StructureValue", Double.NaN);
        assertEquals(AnalysisStatus.UNSUPPORTED, StructMarkerItem.analysisStatus(marker));
    }

    private static CompoundTag exactMarker() {
        CompoundTag marker = new CompoundTag();
        StructMarkerItem.writeAnalysisResult(
                marker,
                new StructureValue(
                        AnalysisStatus.EXACT, 1.0D, 2.0D, 0.0F, new StackMeasure(), List.of()));
        return marker;
    }
}
