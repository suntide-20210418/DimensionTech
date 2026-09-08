package com.suntide_20210418.dimensiontech.loot.expectation;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Immutable marker analysis input shared by processing and expectation-preserving output. */
public record MarkerAnalysis(
        int slot, ItemStack marker, ResourceLocation dimension, BlockPos position,
        double dimensionValue, double structureValue, double quantity,
        Map<ResourceLocation, ExactProbability> expectedItems) {
    public MarkerAnalysis {
        marker = marker.copy();
        expectedItems = Map.copyOf(expectedItems);
    }
}
