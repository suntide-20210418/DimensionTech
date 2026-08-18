package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator.StructureValue;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.IdealRandomProbabilitySpace1201;
import com.suntide_20210418.dimensiontech.utils.loot.expectation.StackMeasure;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runtime-registry tests for the persisted marker analysis payload. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructMarkerItemGameTests {
    private StructMarkerItemGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void exactAnalysisWritesVersionedPayload(GameTestHelper helper) {
        CompoundTag marker = new CompoundTag();
        Diagnostic diagnostic =
                new Diagnostic(
                        "WARNING",
                        "stable warning",
                        ResourceLocation.fromNamespaceAndPath("test", "root"),
                        "/pools/0",
                        List.of("test:root", "test:nested"));
        StructMarkerItem.writeAnalysisResult(
                marker,
                new StructureValue(
                        AnalysisStatus.EXACT,
                        10.0D,
                        42.5D,
                        new StackMeasure(),
                        List.of(diagnostic)));

        ListTag diagnostics = marker.getList("AnalysisDiagnostics", Tag.TAG_COMPOUND);
        CompoundTag saved = diagnostics.getCompound(0);
        if (marker.getInt("StructureValueAlgorithmVersion") != 5
                || StructMarkerItem.analysisStatus(marker) != AnalysisStatus.EXACT
                || marker.getDouble("DimensionValue") != 10.0D
                || marker.getDouble("StructureValue") != 42.5D
                || marker.contains("AnalysisLuck")
                || !IdealRandomProbabilitySpace1201.ID.equals(
                        marker.getString("RandomProbabilitySpace"))
                || diagnostics.size() != 1
                || !"WARNING".equals(saved.getString("Code"))
                || !"stable warning".equals(saved.getString("Message"))
                || !"test:root".equals(saved.getString("LootTable"))
                || !"/pools/0".equals(saved.getString("JsonPointer"))
                || saved.getList("CallPath", Tag.TAG_STRING).size() != 2) {
            helper.fail("Exact marker payload was not persisted stably: " + marker);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void unsupportedAndLegacyMarkersNeverExposeExactValue(GameTestHelper helper) {
        CompoundTag unsupported = new CompoundTag();
        unsupported.putDouble("StructureValue", 999.0D);
        StructMarkerItem.writeAnalysisResult(
                unsupported,
                new StructureValue(
                        AnalysisStatus.UNSUPPORTED,
                        20.0D,
                        999.0D,
                        new StackMeasure(),
                        List.of(new Diagnostic("UNSUPPORTED_TYPE", "reachable mechanism"))));
        CompoundTag legacy = new CompoundTag();
        legacy.putString("AnalysisStatus", AnalysisStatus.EXACT.name());
        legacy.putDouble("StructureValue", 999.0D);
        if (StructMarkerItem.analysisStatus(unsupported) != AnalysisStatus.UNSUPPORTED
                || unsupported.contains("StructureValue", Tag.TAG_ANY_NUMERIC)
                || StructMarkerItem.analysisStatus(legacy) != AnalysisStatus.LEGACY) {
            helper.fail("Unsupported or legacy marker exposed a readable exact value");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void incompleteCurrentPayloadIsUnsupported(GameTestHelper helper) {
        CompoundTag marker = exactMarker();
        marker.remove("RandomProbabilitySpace");
        if (StructMarkerItem.analysisStatus(marker) != AnalysisStatus.UNSUPPORTED) {
            helper.fail("Current payload without probability-space id was accepted");
        }
        marker = exactMarker();
        marker.remove("StructureValue");
        if (StructMarkerItem.analysisStatus(marker) != AnalysisStatus.UNSUPPORTED) {
            helper.fail("Current exact payload without value was accepted");
        }
        marker = exactMarker();
        marker.putDouble("StructureValue", Double.NaN);
        if (StructMarkerItem.analysisStatus(marker) != AnalysisStatus.UNSUPPORTED) {
            helper.fail("Current payload with NaN value was accepted");
        }
        helper.succeed();
    }

    private static CompoundTag exactMarker() {
        CompoundTag marker = new CompoundTag();
        StructMarkerItem.writeAnalysisResult(
                marker,
                new StructureValue(
                        AnalysisStatus.EXACT, 1.0D, 2.0D, new StackMeasure(), List.of()));
        return marker;
    }
}
