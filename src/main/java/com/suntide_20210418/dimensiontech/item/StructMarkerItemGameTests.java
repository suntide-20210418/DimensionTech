package com.suntide_20210418.dimensiontech.item;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.Diagnostic;
import com.suntide_20210418.dimensiontech.loot.expectation.IdealRandomProbabilitySpace1211;
import com.suntide_20210418.dimensiontech.loot.expectation.StackMeasure;
import com.suntide_20210418.dimensiontech.structure.analysis.StructureValueCalculator.StructureValue;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 已持久化标记分析负载的运行时测试。
 *
 * <p>负载现在是挂在 data component 上的 {@link StructureMarkerData} record，所以测试改为驱动 {@link
 * StructMarkerItem#withAnalysis}（把分析结果合并进负载的写入器），再从已标记的物品栈上读回负载做断言。
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructMarkerItemGameTests {
    private static final ResourceLocation TEST_LOOT_TABLE =
            ResourceLocation.fromNamespaceAndPath("test", "root");
    private static final ResourceLocation TEST_STRUCTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "jungle_pyramid");

    private StructMarkerItemGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void exactAnalysisWritesVersionedPayload(GameTestHelper helper) {
        Diagnostic diagnostic =
                new Diagnostic(
                        "WARNING",
                        "stable warning",
                        TEST_LOOT_TABLE,
                        "/pools/0",
                        List.of("test:root", "test:nested"));

        ItemStack stack = new ItemStack(ModItems.STRUCTURE_MARKER.get());
        stack.set(
                ModDataComponents.STRUCTURE_MARKER,
                analysed(
                        helper,
                        new StructureValue(
                                AnalysisStatus.EXACT,
                                10.0D,
                                42.5D,
                                new StackMeasure(),
                                List.of(diagnostic))));

        // 下游只通过组件读负载，所以这里也从组件读回，而不是去检查 tag。
        StructureMarkerData marker = StructMarkerItem.getMarkerData(stack).orElse(null);
        if (marker == null
                || marker.algorithmVersion() != StructMarkerItem.ALGORITHM_VERSION
                || marker.status() != AnalysisStatus.EXACT
                || marker.dimensionValue() != 10.0D
                || marker.structureValue().orElse(-1.0D) != 42.5D
                || !IdealRandomProbabilitySpace1211.ID.equals(marker.randomProbabilitySpace())
                || marker.diagnostics().size() != 1) {
            helper.fail("Exact marker payload was not persisted stably: " + marker);
            return;
        }
        Diagnostic saved = marker.diagnostics().get(0);
        if (!"WARNING".equals(saved.code())
                || !"stable warning".equals(saved.message())
                || !TEST_LOOT_TABLE.equals(saved.lootTableId())
                || !"/pools/0".equals(saved.jsonPointer())
                || saved.callPath().size() != 2) {
            helper.fail("Exact marker payload lost its diagnostic payload: " + saved);
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void unsupportedAndLegacyMarkersNeverExposeExactValue(GameTestHelper helper) {
        StructureMarkerData unsupported =
                analysed(
                        helper,
                        new StructureValue(
                                AnalysisStatus.UNSUPPORTED,
                                20.0D,
                                999.0D,
                                new StackMeasure(),
                                List.of(
                                        new Diagnostic(
                                                "UNSUPPORTED_TYPE", "reachable mechanism"))));
        // 由其它写入器盖章的负载一律算 legacy，无论它自称什么状态。
        StructureMarkerData legacy =
                withAlgorithmVersion(
                        analysed(
                                helper,
                                new StructureValue(
                                        AnalysisStatus.EXACT,
                                        20.0D,
                                        999.0D,
                                        new StackMeasure(),
                                        List.of())),
                        StructMarkerItem.ALGORITHM_VERSION - 1);
        if (unsupported.status() != AnalysisStatus.UNSUPPORTED
                || unsupported.structureValue().isPresent()
                || legacy.status() != AnalysisStatus.LEGACY) {
            helper.fail("Unsupported or legacy marker exposed a readable exact value");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void incompleteCurrentPayloadIsUnsupported(GameTestHelper helper) {
        StructureMarkerData marker =
                analysed(
                        helper,
                        new StructureValue(
                                AnalysisStatus.EXACT, 1.0D, 2.0D, new StackMeasure(), List.of()));
        if (withPayload(marker, "", marker.structureValue(), marker.algorithmVersion()).status()
                != AnalysisStatus.UNSUPPORTED) {
            helper.fail("Current payload without probability-space id was accepted");
            return;
        }
        if (withPayload(
                                marker,
                                marker.randomProbabilitySpace(),
                                Optional.empty(),
                                marker.algorithmVersion())
                        .status()
                != AnalysisStatus.UNSUPPORTED) {
            helper.fail("Current exact payload without value was accepted");
            return;
        }
        if (withPayload(
                                marker,
                                marker.randomProbabilitySpace(),
                                Optional.of(Double.NaN),
                                marker.algorithmVersion())
                        .status()
                != AnalysisStatus.UNSUPPORTED) {
            helper.fail("Current payload with NaN value was accepted");
            return;
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void markerReadsExactlyOneRecordedStructure(GameTestHelper helper) {
        /*
         * 1.20.1 的标记可以带一个 legacy 的 "Structures" 列表，读取端会从中确定性地挑出一个结构。
         * 那个读取端随 tag 一起消失了（负载按构造只记一个结构），所以这里能钉住的不变量只剩：一个已标记的
         * 物品栈只能解析回它被写入时的那一份身份，不会有第二个。
         */
        BlockPos position = new BlockPos(11, 64, -7);
        ItemStack marker = new ItemStack(ModItems.STRUCTURE_MARKER.get());
        marker.set(
                ModDataComponents.STRUCTURE_MARKER,
                new StructureMarkerData(
                        helper.getLevel().dimension().location(),
                        position,
                        new StructMarkerItem.MarkedStructure(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "ancient_city"),
                                new BoundingBox(11, 64, -7, 16, 69, -2)),
                        Optional.empty(),
                        StructMarkerItem.ALGORITHM_VERSION,
                        IdealRandomProbabilitySpace1211.ID,
                        0.0D,
                        Optional.empty(),
                        AnalysisStatus.LEGACY,
                        List.of(),
                        List.of(),
                        "test-fingerprint",
                        false));

        StructMarkerItem.MarkerInfo info = StructMarkerItem.getMarkerInfo(marker).orElse(null);
        if (info == null
                || !position.equals(info.position())
                || !ResourceLocation.fromNamespaceAndPath("minecraft", "ancient_city")
                        .equals(info.structure().id())) {
            helper.fail("Marker did not resolve to exactly one deterministic structure");
            return;
        }
        helper.succeed();
    }

    /** 驱动共享写入器，把一个分析结果合并进新的身份负载。 */
    private static StructureMarkerData analysed(GameTestHelper helper, StructureValue value) {
        StructMarkerItem.MarkerInfo info =
                new StructMarkerItem.MarkerInfo(
                        helper.getLevel().dimension().location(),
                        BlockPos.ZERO,
                        new StructMarkerItem.MarkedStructure(
                                TEST_STRUCTURE, new BoundingBox(0, 0, 0, 0, 0, 0)));
        return StructMarkerItem.withAnalysis(
                StructMarkerItem.baseMarkerData(info, false, Optional.empty()),
                value,
                "test-fingerprint");
    }

    /** 只改算法版本的负载重建。 */
    private static StructureMarkerData withAlgorithmVersion(
            StructureMarkerData data, int algorithmVersion) {
        return withPayload(
                data, data.randomProbabilitySpace(), data.structureValue(), algorithmVersion);
    }

    /** 重建负载，改掉校验闸门会读的那三个字段。 */
    private static StructureMarkerData withPayload(
            StructureMarkerData data,
            String randomProbabilitySpace,
            Optional<Double> structureValue,
            int algorithmVersion) {
        return new StructureMarkerData(
                data.dimension(),
                data.position(),
                data.structure(),
                data.chestData(),
                algorithmVersion,
                randomProbabilitySpace,
                data.dimensionValue(),
                structureValue,
                data.analysisStatus(),
                data.diagnostics(),
                data.expectedItemCounts(),
                data.analysisFingerprint(),
                data.catalogueEntry());
    }
}
