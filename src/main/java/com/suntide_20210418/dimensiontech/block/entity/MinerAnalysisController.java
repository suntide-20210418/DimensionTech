package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.items.ItemStackHandler;

/** Owns marker-analysis identity and the explicit asynchronous analysis refresh lifecycle. */
final class MinerAnalysisController {
    private final ItemStackHandler inventory;
    private final Supplier<Float> luck;
    private final MythicMinerMarkerAnalysisCache cache;

    MinerAnalysisController(ItemStackHandler inventory, Supplier<Float> luck, BooleanSupplier removed) {
        this.inventory = inventory;
        this.luck = luck;
        this.cache = new MythicMinerMarkerAnalysisCache(inventory, this::fingerprint, removed);
    }

    List<Integer> refresh(MinecraftServer server, long gameTime) {
        return cache.refresh(server, luck.get(), gameTime);
    }

    void invalidateIfInputsChanged() { cache.invalidateIfAnalysisInputsChanged(); }
    List<MythicMinerMarkerAnalysisCache.CachedMarkerLoot> entries() { return cache.entries(); }
    MythicMinerMarkerAnalysisCache.CachedMarkerLoot entryForSlot(int slot) { return cache.entryForSlot(slot); }
    String cacheStatus(int slot) { return cache.cacheStatus(slot); }
    AnalysisLifecycle.TaskStatus taskStatus(int slot) { return cache.taskStatus(slot); }

    private LootAnalysisFingerprint fingerprint() {
        return LootAnalysisFingerprint.from(
                inventory, luck.get(), ModConfigs.STRUCTURE_VALUE.calculationFingerprint());
    }
}
