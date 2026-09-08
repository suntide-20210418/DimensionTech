package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.items.ItemStackHandler;

/** Owns marker-analysis identity and the explicit asynchronous analysis refresh lifecycle. */
final class MinerAnalysisController {
    private final ItemStackHandler inventory;
    private final Supplier<Float> luck;
    private final MythicMinerMarkerAnalysisCache cache;
    private Map<Integer, MythicMinerUpgradeMath.ProcessingPlan> plans = Map.of();

    MinerAnalysisController(
            ItemStackHandler inventory, Supplier<Float> luck, BooleanSupplier removed) {
        this.inventory = inventory;
        this.luck = luck;
        this.cache = new MythicMinerMarkerAnalysisCache(inventory, this::fingerprint, removed);
    }

    List<Integer> refresh(MinecraftServer server, long gameTime) {
        return cache.refresh(server, luck.get(), gameTime);
    }

    void invalidateIfInputsChanged() {
        cache.invalidateIfAnalysisInputsChanged();
    }

    List<MythicMinerMarkerAnalysisCache.CachedMarkerLoot> entries() {
        return cache.entries();
    }

    MythicMinerMarkerAnalysisCache.CachedMarkerLoot entryForSlot(int slot) {
        return cache.entryForSlot(slot);
    }

    String cacheStatus(int slot) {
        return cache.cacheStatus(slot);
    }

    AnalysisLifecycle.TaskStatus taskStatus(int slot) {
        return cache.taskStatus(slot);
    }

    void refreshPlans(double efficiency, int configuredProcessingTime, int minimumNaturalTicks,
            int baseParallel, boolean[] enabledSlots) {
        Map<Integer, MythicMinerUpgradeMath.ProcessingPlan> next = new HashMap<>();
        for (MythicMinerMarkerAnalysisCache.CachedMarkerLoot loot : cache.entries()) {
            int slot = loot.slot();
            if (slot >= 0 && slot < enabledSlots.length && enabledSlots[slot]) {
                next.put(slot, MythicMinerUpgradeMath.processingPlan(loot.structureValue(), efficiency,
                        configuredProcessingTime, minimumNaturalTicks, baseParallel));
            }
        }
        plans = Map.copyOf(next);
    }

    MythicMinerUpgradeMath.ProcessingPlan plan(int slot) {
        return plans.getOrDefault(slot, new MythicMinerUpgradeMath.ProcessingPlan(0, 0));
    }

    private LootAnalysisFingerprint fingerprint() {
        return LootAnalysisFingerprint.from(
                inventory, luck.get(), ModConfigs.STRUCTURE_VALUE.calculationFingerprint());
    }
}
