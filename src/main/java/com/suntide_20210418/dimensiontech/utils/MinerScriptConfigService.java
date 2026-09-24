package com.suntide_20210418.dimensiontech.utils;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Immutable-at-read configuration facade; cleared on server reload by the KubeJS adapter. */
public final class MinerScriptConfigService {
    private static volatile Map<ResourceLocation, MinerScriptConfig> configs = Map.of();

    private MinerScriptConfigService() {}

    public static synchronized void clear() {
        configs = Map.of();
    }

    public static synchronized void put(MinerScriptConfig config) {
        Map<ResourceLocation, MinerScriptConfig> next = new HashMap<>(configs);
        next.put(config.blockId(), config);
        configs = Map.copyOf(next);
    }

    public static synchronized MinerScriptConfig merge(
            ResourceLocation id,
            Integer processingTime,
            Integer energyConsumption,
            Integer energyCapacity,
            Integer baseParallel,
            Double efficiency,
            Float luck,
            Boolean requiresFluid) {
        MinerScriptConfig old = configs.get(id);
        MinerScriptConfig next =
                new MinerScriptConfig(
                        id,
                        processingTime != null
                                ? processingTime
                                : old == null ? null : old.processingTime(),
                        energyConsumption != null
                                ? energyConsumption
                                : old == null ? null : old.energyConsumption(),
                        energyCapacity != null
                                ? energyCapacity
                                : old == null ? null : old.energyCapacity(),
                        baseParallel != null
                                ? baseParallel
                                : old == null ? null : old.baseParallel(),
                        efficiency != null ? efficiency : old == null ? null : old.efficiency(),
                        luck != null ? luck : old == null ? null : old.luck(),
                        requiresFluid != null
                                ? requiresFluid
                                : old == null ? null : old.requiresFluid());
        put(next);
        return next;
    }

    public static MinerScriptConfig get(ResourceLocation id) {
        return configs.get(id);
    }

    public static Map<ResourceLocation, MinerScriptConfig> snapshot() {
        return configs;
    }
}
