package com.suntide_20210418.dimensiontech.loot.fingerprint;

import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Inputs whose equality means that a cached marker analysis is safe to reuse. This is deliberately
 * local to miner analysis and is not a general cache key.
 */
public record LootAnalysisFingerprint(
        int algorithmVersion, List<String> markerSlots, int luckBits, String analysisConfig) {
    public static final int ALGORITHM_VERSION = 1;

    LootAnalysisFingerprint {
        markerSlots = List.copyOf(markerSlots);
        analysisConfig = analysisConfig == null ? "" : analysisConfig;
    }

    public static LootAnalysisFingerprint from(
            ItemStackHandler itemHandler, float effectiveLuck, String analysisConfig) {
        List<String> slots = new ArrayList<>(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            var marker = StructMarkerItem.getMarkerInfo(stack);
            if (marker.isPresent()) slots.add(slotFingerprint(slot, marker.get()));
        }
        return new LootAnalysisFingerprint(
                ALGORITHM_VERSION, slots, Float.floatToIntBits(effectiveLuck), analysisConfig);
    }

    private static String slotFingerprint(int slot, MarkerInfo marker) {
        var bounds = marker.structure().bounds();
        return slot
                + ":"
                + marker.dimension()
                + ":"
                + marker.position().getX()
                + ":"
                + marker.position().getY()
                + ":"
                + marker.position().getZ()
                + ":"
                + marker.structure().id()
                + ":"
                + bounds.minX()
                + ":"
                + bounds.minY()
                + ":"
                + bounds.minZ()
                + ":"
                + bounds.maxX()
                + ":"
                + bounds.maxY()
                + ":"
                + bounds.maxZ();
    }
}
