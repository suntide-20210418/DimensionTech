package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.DimensionTechMod;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemStackHandler;

/** Runtime checks for the miner-specific cache reuse invariant. */
@GameTestHolder(DimensionTechMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LootAnalysisFingerprintGameTests {
    private LootAnalysisFingerprintGameTests() {}

    @GameTest(templateNamespace = "minecraft", template = "empty")
    public static void onlyAnalysisInputsChangeTheMarkerFingerprint(GameTestHelper helper) {
        ItemStack original = marker(helper);
        ItemStack countChanged = original.copy();
        countChanged.setCount(32);
        ItemStack analysisPayloadChanged = original.copy();
        analysisPayloadChanged
                .getTag()
                .getCompound("StructureMarkerData")
                .putDouble("DimensionValue", 9999.0D);
        ItemStack positionChanged = original.copy();
        positionChanged
                .getTag()
                .getCompound("StructureMarkerData")
                .getCompound("Position")
                .putInt("X", 42);
        ItemStack boundsChanged = original.copy();
        boundsChanged
                .getTag()
                .getCompound("StructureMarkerData")
                .getCompound("Structure")
                .getCompound("Bounds")
                .putInt("MaxX", 42);
        ItemStack dimensionChanged = original.copy();
        dimensionChanged
                .getTag()
                .getCompound("StructureMarkerData")
                .putString("Dimension", "minecraft:the_nether");

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
        marker.getOrCreateTag().put(
                "StructureMarkerData",
                StructMarkerItem.createCatalogueMarkerData(
                        helper.getLevel(),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "jungle_pyramid")));
        return marker;
    }

    private static LootAnalysisFingerprint fingerprint(ItemStack marker) {
        return fingerprint(marker, 0.0F, "test-config");
    }

    private static LootAnalysisFingerprint fingerprint(ItemStack marker, float luck, String config) {
        ItemStackHandler inventory = new ItemStackHandler(1);
        inventory.setStackInSlot(0, marker);
        return LootAnalysisFingerprint.from(inventory, luck, config);
    }
}
