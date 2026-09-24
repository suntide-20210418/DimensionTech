package com.suntide_20210418.dimensiontech.block.entity;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Server-authoritative marker analysis after the miner's effective attributes are applied. */
public record StructureMinerAnalysisSnapshot(
        double dimensionValue,
        double structureValue,
        boolean equipmentDismantling,
        Map<ResourceLocation, Double> itemExpectations,
        Set<ResourceLocation> disabledItems) {
    public static final StructureMinerAnalysisSnapshot EMPTY =
            new StructureMinerAnalysisSnapshot(0.0D, 0.0D, false, Map.of(), Set.of());

    public StructureMinerAnalysisSnapshot {
        itemExpectations = Map.copyOf(itemExpectations);
        disabledItems = Set.copyOf(disabledItems);
    }
}
