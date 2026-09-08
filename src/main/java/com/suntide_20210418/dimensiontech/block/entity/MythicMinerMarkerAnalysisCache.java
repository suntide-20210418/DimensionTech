package com.suntide_20210418.dimensiontech.block.entity;

import com.suntide_20210418.dimensiontech.config.ModConfigs;
import com.suntide_20210418.dimensiontech.item.ModItems;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem;
import com.suntide_20210418.dimensiontech.item.StructMarkerItem.MarkerInfo;
import com.suntide_20210418.dimensiontech.loot.expectation.AnalysisStatus;
import com.suntide_20210418.dimensiontech.loot.expectation.ExactProbability;
import com.suntide_20210418.dimensiontech.loot.expectation.RuntimeLootAstSource;
import com.suntide_20210418.dimensiontech.utils.AnalysisLifecycle;
import com.suntide_20210418.dimensiontech.utils.StructureAnalysisService;
import com.suntide_20210418.dimensiontech.utils.StructureValueCalculator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Owns the miner-specific Marker analysis lifecycle.
 *
 * <p>The submitted and completion-time fingerprints must match before an async result is accepted.
 * Consequently, a completed entry can only be reused while its analysis inputs remain equal. This
 * is deliberately not a general-purpose task cache.
 */
final class MythicMinerMarkerAnalysisCache {
    private static final long RETRY_DELAY_TICKS = 100L;

    private final ItemStackHandler inventory;
    private final Supplier<LootAnalysisFingerprint> currentFingerprint;
    private final BooleanSupplier removed;
    private final AnalysisStarter analysisStarter;
    private LootAnalysisFingerprint analyzedFingerprint;
    private List<CachedMarkerLoot> cachedLoot = List.of();
    private final Map<Integer, CompletableFuture<StructureValueCalculator.StructureValue>> pending =
            new LinkedHashMap<>();
    private final Map<Integer, AnalysisLifecycle.TaskStatus> tasks = new LinkedHashMap<>();
    private long generation;
    private long retryAt;

    MythicMinerMarkerAnalysisCache(
            ItemStackHandler inventory,
            Supplier<LootAnalysisFingerprint> currentFingerprint,
            BooleanSupplier removed) {
        this(inventory, currentFingerprint, removed, MythicMinerMarkerAnalysisCache::startAnalysis);
    }

    MythicMinerMarkerAnalysisCache(
            ItemStackHandler inventory,
            Supplier<LootAnalysisFingerprint> currentFingerprint,
            BooleanSupplier removed,
            AnalysisStarter analysisStarter) {
        this.inventory = inventory;
        this.currentFingerprint = currentFingerprint;
        this.removed = removed;
        this.analysisStarter = analysisStarter;
    }

    /**
     * Refreshes only when analysis inputs changed or a failed analysis becomes eligible to retry.
     */
    List<Integer> refresh(MinecraftServer server, float luck, long gameTime) {
        LootAnalysisFingerprint fingerprint = currentFingerprint.get();
        boolean retry =
                gameTime >= retryAt && tasks.containsValue(AnalysisLifecycle.TaskStatus.FAILED);
        if (fingerprint.equals(analyzedFingerprint) && !retry) return List.of();

        long refreshGeneration = ++generation;
        List<Integer> invalidatedSlots = new ArrayList<>();
        List<CachedMarkerLoot> refreshed = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack marker = inventory.getStackInSlot(slot);
            if (!marker.is(ModItems.STRUCTURE_MARKER.get())) {
                invalidate(slot, invalidatedSlots);
                continue;
            }
            CachedMarkerLoot previous = cachedLootForSlot(slot);
            if (previous == null || !ItemStack.isSameItemSameTags(previous.marker(), marker)) {
                invalidate(slot, invalidatedSlots);
                previous = null;
            }
            var info = StructMarkerItem.getMarkerInfo(marker);
            if (info.isEmpty()) {
                invalidate(slot, invalidatedSlots);
                continue;
            }
            request(
                    server,
                    refreshGeneration,
                    fingerprint,
                    slot,
                    marker,
                    info.get(),
                    luck,
                    gameTime);
            if (previous != null) refreshed.add(previous);
        }
        cachedLoot = List.copyOf(refreshed);
        analyzedFingerprint = fingerprint;
        return List.copyOf(invalidatedSlots);
    }

    List<CachedMarkerLoot> entries() {
        return cachedLoot;
    }

    CachedMarkerLoot entryForSlot(int slot) {
        return cachedLootForSlot(slot);
    }

    String cacheStatus(int slot) {
        CachedMarkerLoot cached = cachedLootForSlot(slot);
        if (cached == null) return AnalysisLifecycle.CacheStatus.MISSING.name();
        return pending.containsKey(slot) || tasks.get(slot) == AnalysisLifecycle.TaskStatus.FAILED
                ? AnalysisLifecycle.CacheStatus.STALE.name()
                : AnalysisLifecycle.CacheStatus.COMPLETE.name();
    }

    AnalysisLifecycle.TaskStatus taskStatus(int slot) {
        return tasks.getOrDefault(slot, AnalysisLifecycle.TaskStatus.SUCCEEDED);
    }

    /** Cancels stale lifecycle state only when the cache-reuse invariant no longer holds. */
    void invalidateIfAnalysisInputsChanged() {
        if (analyzedFingerprint == null || analyzedFingerprint.equals(currentFingerprint.get()))
            return;
        generation++;
        pending.clear();
        tasks.clear();
        cachedLoot = List.of();
        analyzedFingerprint = null;
    }

    private void invalidate(int slot, List<Integer> invalidatedSlots) {
        if (!invalidatedSlots.contains(slot)) invalidatedSlots.add(slot);
        pending.remove(slot);
        tasks.remove(slot);
    }

    private void request(
            MinecraftServer server,
            long requestGeneration,
            LootAnalysisFingerprint requestFingerprint,
            int slot,
            ItemStack marker,
            MarkerInfo info,
            float luck,
            long gameTime) {
        tasks.put(slot, AnalysisLifecycle.TaskStatus.QUEUED);
        ResourceKey<Level> dimensionKey =
                ResourceKey.create(Registries.DIMENSION, info.dimension());
        ServerLevel analysisLevel = server.getLevel(dimensionKey);
        if (analysisLevel == null) {
            tasks.put(slot, AnalysisLifecycle.TaskStatus.FAILED);
            retryAt = gameTime + RETRY_DELAY_TICKS;
            return;
        }
        ItemStack requestedMarker = marker.copy();
        CompletableFuture<StructureValueCalculator.StructureValue> future =
                analysisStarter.start(server, analysisLevel, info, luck);
        pending.put(slot, future);
        tasks.put(slot, AnalysisLifecycle.TaskStatus.RUNNING);
        future.whenComplete(
                (analysis, error) ->
                        server.tell(
                                new TickTask(
                                        server.getTickCount(),
                                        () ->
                                                finish(
                                                        requestGeneration,
                                                        requestFingerprint,
                                                        slot,
                                                        requestedMarker,
                                                        info,
                                                        future,
                                                        analysis,
                                                        error,
                                                        gameTime))));
    }

    private void finish(
            long requestGeneration,
            LootAnalysisFingerprint requestFingerprint,
            int slot,
            ItemStack requestedMarker,
            MarkerInfo info,
            CompletableFuture<StructureValueCalculator.StructureValue> future,
            StructureValueCalculator.StructureValue analysis,
            Throwable error,
            long requestTime) {
        if (requestGeneration != generation
                || !requestFingerprint.equals(currentFingerprint.get())
                || removed.getAsBoolean()
                || !markerStillPresent(slot, requestedMarker)) {
            return;
        }
        pending.remove(slot, future);
        if (error != null
                || analysis == null
                || (analysis.status() != AnalysisStatus.EXACT
                        && analysis.status() != AnalysisStatus.APPROXIMATE)) {
            tasks.put(slot, AnalysisLifecycle.TaskStatus.FAILED);
            retryAt = requestTime + RETRY_DELAY_TICKS;
            return;
        }
        tasks.put(slot, AnalysisLifecycle.TaskStatus.SUCCEEDED);
        replace(slot, requestedMarker, info, analysis);
    }

    private boolean markerStillPresent(int slot, ItemStack marker) {
        return slot >= 0
                && slot < inventory.getSlots()
                && ItemStack.isSameItemSameTags(inventory.getStackInSlot(slot), marker);
    }

    private CachedMarkerLoot cachedLootForSlot(int slot) {
        for (CachedMarkerLoot entry : cachedLoot) {
            if (entry.slot() == slot) return entry;
        }
        return null;
    }

    private static CompletableFuture<StructureValueCalculator.StructureValue> startAnalysis(
            MinecraftServer server, ServerLevel analysisLevel, MarkerInfo info, float luck) {
        String input = info.dimension() + "|" + info.position() + "|" + info.structure().id();
        String config = ModConfigs.STRUCTURE_VALUE.calculationFingerprint() + "|luck=" + luck;
        return StructureAnalysisService.forServer(server)
                .computeAsync(
                        "miner-structure-value",
                        input,
                        config,
                        LootAnalysisFingerprint.ALGORITHM_VERSION,
                        () ->
                                StructureAnalysisService.forServer(server)
                                        .discover(analysisLevel, info.structure().id())
                                        .thenComposeAsync(
                                                discovery -> {
                                                    List<ResourceLocation> roots =
                                                            discovery.structures().stream()
                                                                    .flatMap(
                                                                            value ->
                                                                                    value
                                                                                            .lootTables()
                                                                                            .stream())
                                                                    .distinct()
                                                                    .toList();
                                                    RuntimeLootAstSource source =
                                                            RuntimeLootAstSource.snapshotTables(
                                                                    server, roots);
                                                    return StructureValueCalculator.calculateAsync(
                                                            server, info, luck, discovery, source);
                                                },
                                                server));
    }

    @FunctionalInterface
    interface AnalysisStarter {
        CompletableFuture<StructureValueCalculator.StructureValue> start(
                MinecraftServer server, ServerLevel level, MarkerInfo info, float luck);
    }

    private void replace(
            int slot,
            ItemStack marker,
            MarkerInfo info,
            StructureValueCalculator.StructureValue analysis) {
        Map<ResourceLocation, ExactProbability> expectedItems = new LinkedHashMap<>();
        analysis.itemCounts()
                .forEach(
                        (item, expected) -> {
                            ResourceLocation id =
                                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                            item);
                            if (id != null) expectedItems.put(id, expected);
                        });
        CachedMarkerLoot entry =
                new CachedMarkerLoot(
                        slot,
                        marker.copy(),
                        info.dimension(),
                        info.position(),
                        analysis.dimensionValue(),
                        analysis.structureValue(),
                        analysis.itemCounts().values().stream()
                                .mapToDouble(ExactProbability::finiteDoubleValue)
                                .filter(Double::isFinite)
                                .filter(count -> count > 0.0D)
                                .sum(),
                        expectedItems);
        List<CachedMarkerLoot> updated = new ArrayList<>(cachedLoot);
        updated.removeIf(value -> value.slot() == slot);
        updated.add(entry);
        cachedLoot = List.copyOf(updated);
    }

    record CachedMarkerLoot(
            int slot,
            ItemStack marker,
            ResourceLocation dimension,
            BlockPos position,
            double dimensionValue,
            double structureValue,
            double quantity,
            Map<ResourceLocation, ExactProbability> expectedItems) {}
}
