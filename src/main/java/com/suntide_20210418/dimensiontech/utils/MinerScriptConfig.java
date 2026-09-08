package com.suntide_20210418.dimensiontech.utils;

import com.suntide_20210418.dimensiontech.mythicminer.processing.ExternalTickAcceleration;
import net.minecraft.resources.ResourceLocation;

/** Server-scoped, optional overrides supplied by integrations such as KubeJS. */
public record MinerScriptConfig(
        ResourceLocation blockId,
        Integer processingTime,
        Integer energyConsumption,
        Integer energyCapacity,
        Integer baseParallel,
        Double efficiency,
        Float luck,
        Boolean requiresFluid) {
    public MinerScriptConfig {
        if (blockId == null) throw new IllegalArgumentException("blockId is required");
        if (processingTime != null && processingTime < ExternalTickAcceleration.MINIMUM_NATURAL_TICKS)
            throw new IllegalArgumentException(
                    "processingTime must be at least "
                            + ExternalTickAcceleration.MINIMUM_NATURAL_TICKS);
        if (energyConsumption != null && energyConsumption < 0)
            throw new IllegalArgumentException("energyConsumption must be non-negative");
        if (energyCapacity != null && energyCapacity < 0)
            throw new IllegalArgumentException("energyCapacity must be non-negative");
        if (baseParallel != null && baseParallel < 1)
            throw new IllegalArgumentException("baseParallel must be positive");
        if (efficiency != null && (!Double.isFinite(efficiency) || efficiency < 0))
            throw new IllegalArgumentException("efficiency must be finite and non-negative");
        if (luck != null && (!Float.isFinite(luck) || luck < 0))
            throw new IllegalArgumentException("luck must be finite and non-negative");
    }
}
