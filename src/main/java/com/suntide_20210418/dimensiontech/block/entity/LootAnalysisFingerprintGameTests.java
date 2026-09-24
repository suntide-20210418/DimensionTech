package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModDataComponents;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.item.StructureMarkerData;
import com.suntide_20210418.dimensiontech.loot.fingerprint.LootAnalysisFingerprint;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 采掘器专用缓存复用不变量的运行时检查。
 *
 * <p>标记负载现在是不可变 record，所以「改过的标记」不再是就地改 tag，而是换一份重建过的负载。指纹只吃身份字段（dimension、position、结构 id 与
 * bounds），与它们并排存放的分析结果不得进入指纹。
 */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LootAnalysisFingerprintGameTests {
    private LootAnalysisFingerprintGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void onlyAnalysisInputsChangeTheMarkerFingerprint(GameTestHelper helper) {
        ItemStack original = marker(helper);
        StructureMarkerData data = StructMarkerItem.getMarkerData(original).orElseThrow();
        BoundingBox bounds = data.structure().bounds();

        ItemStack countChanged = original.copy();
        countChanged.setCount(32);
        ItemStack analysisPayloadChanged =
                withPayload(original, data.dimension(), data.position(), data.structure(), 9999.0D);
        ItemStack positionChanged =
                withPayload(
                        original,
                        data.dimension(),
                        new BlockPos(42, data.position().getY(), data.position().getZ()),
                        data.structure(),
                        data.dimensionValue());
        ItemStack boundsChanged =
                withPayload(
                        original,
                        data.dimension(),
                        data.position(),
                        new StructMarkerItem.MarkedStructure(
                                data.structure().id(),
                                new BoundingBox(
                                        bounds.minX(),
                                        bounds.minY(),
                                        bounds.minZ(),
                                        42,
                                        bounds.maxY(),
                                        bounds.maxZ())),
                        data.dimensionValue());
        ItemStack dimensionChanged =
                withPayload(
                        original,
                        ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether"),
                        data.position(),
                        data.structure(),
                        data.dimensionValue());

        LootAnalysisFingerprint baseline = fingerprint(original);
        if (!baseline.equals(fingerprint(countChanged))
                || !baseline.equals(fingerprint(analysisPayloadChanged))
                || baseline.equals(fingerprint(positionChanged))
                || baseline.equals(fingerprint(boundsChanged))
                || baseline.equals(fingerprint(dimensionChanged))
                || baseline.equals(fingerprint(original, 1.0F, "test-config"))
                || baseline.equals(fingerprint(original, 0.0F, "changed-config"))) {
            helper.fail("Marker fingerprint did not distinguish only analysis-relevant inputs");
            return;
        }
        helper.succeed();
    }

    private static ItemStack marker(GameTestHelper helper) {
        ItemStack marker = new ItemStack(ModItems.STRUCTURE_MARKER.get());
        marker.set(
                ModDataComponents.STRUCTURE_MARKER,
                StructMarkerItem.createCatalogueMarkerData(
                        helper.getLevel(),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "jungle_pyramid")));
        return marker;
    }

    /** {@code marker} 的副本，只有给定的四个字段与它不同。 */
    private static ItemStack withPayload(
            ItemStack marker,
            ResourceLocation dimension,
            BlockPos position,
            StructMarkerItem.MarkedStructure structure,
            double dimensionValue) {
        ItemStack changed = marker.copy();
        StructureMarkerData data = StructMarkerItem.getMarkerData(changed).orElseThrow();
        changed.set(
                ModDataComponents.STRUCTURE_MARKER,
                new StructureMarkerData(
                        dimension,
                        position,
                        structure,
                        data.chestData(),
                        data.algorithmVersion(),
                        data.randomProbabilitySpace(),
                        dimensionValue,
                        data.structureValue(),
                        data.analysisStatus(),
                        data.diagnostics(),
                        data.expectedItemCounts(),
                        data.analysisFingerprint(),
                        data.catalogueEntry()));
        return changed;
    }

    private static LootAnalysisFingerprint fingerprint(ItemStack marker) {
        return fingerprint(marker, 0.0F, "test-config");
    }

    private static LootAnalysisFingerprint fingerprint(
            ItemStack marker, float luck, String config) {
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, marker);
        return LootAnalysisFingerprint.from(inventory, luck, config);
    }
}
